/**
 * spike_test.js — Sudden traffic burst to test service resilience.
 *
 * Scenario:
 *   - System is idle
 *   - Traffic spikes from 0 → 50 VUs in 10 seconds (simulating viral event / DDoS)
 *   - Holds at 50 VUs for 30 seconds
 *   - Drops back to 0 in 10 seconds
 *
 * What we're testing:
 *   - Does the Dispatcher hold up under sudden load?
 *   - Does the retry policy fire under stress?
 *   - Does error rate stay below 5% during the spike?
 *   - Do p95 response times stay below 2s (lenient — it's a spike)?
 *
 * Run:
 *   k6 run k6/spike_test.js
 *   k6 run --out influxdb=http://localhost:8086/k6 k6/spike_test.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { BASE_URL, jsonParams, authParams } from './helpers.js';

// ── Custom metrics ────────────────────────────────────────────────────────────
const loginDuration  = new Trend('login_duration_ms',  true);
const ideasDuration  = new Trend('ideas_list_duration_ms', true);
const offersDuration = new Trend('offers_create_duration_ms', true);
const errorRate      = new Rate('errors');
const totalRequests  = new Counter('total_requests');

// ── Load shape ────────────────────────────────────────────────────────────────
export const options = {
    scenarios: {
        spike: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 50 },   // ramp up sharply
                { duration: '30s', target: 50 },   // hold at peak
                { duration: '10s', target: 0  },   // ramp down
            ],
        },
    },

    thresholds: {
        // Core SLOs — must hold even during spike
        http_req_failed:        ['rate<0.05'],          // < 5% HTTP errors
        http_req_duration:      ['p(95)<2000'],         // p95 < 2s
        checks:                 ['rate>0.95'],          // 95%+ checks pass

        // Custom metrics
        login_duration_ms:      ['p(95)<1000'],         // login p95 < 1s
        ideas_list_duration_ms: ['p(95)<1500'],         // list ideas p95 < 1.5s
        errors:                 ['rate<0.05'],
    },
};

// ── Setup: register shared test users once ────────────────────────────────────
export function setup() {
    const ts       = Date.now();
    const password = 'Test1234!';

    // Register a founder
    const fEmail = `spike_founder_${ts}@test.com`;
    const rF = http.post(`${BASE_URL}/auth/register`,
        JSON.stringify({ email: fEmail, password, role: 'FOUNDER' }), jsonParams());
    if (rF.status !== 201) {
        console.error(`Founder registration failed: ${rF.status} ${rF.body}`);
    }

    // Register an investor
    const iEmail = `spike_investor_${ts}@test.com`;
    http.post(`${BASE_URL}/auth/register`,
        JSON.stringify({ email: iEmail, password, role: 'INVESTOR' }), jsonParams());

    // Login both
    const fLogin = http.post(`${BASE_URL}/auth/login`,
        JSON.stringify({ email: fEmail, password }), jsonParams());
    const iLogin = http.post(`${BASE_URL}/auth/login`,
        JSON.stringify({ email: iEmail, password }), jsonParams());

    const founderBody  = JSON.parse(fLogin.body);
    const investorBody = JSON.parse(iLogin.body);

    // Create a draft idea for offer tests
    let ideaId   = null;
    let founderId = founderBody.userId || null;
    const ideaRes = http.post(`${BASE_URL}/ideas`,
        JSON.stringify({
            title:       'Spike Test Idea',
            description: 'Created in setup for spike test',
            sector:      'Fintech',
            fundingGoal: 100000,
        }),
        { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${founderBody.token}` } },
    );
    if (ideaRes.status === 201) {
        ideaId    = JSON.parse(ideaRes.body).id;
        founderId = JSON.parse(ideaRes.body).founderId || founderId;
    }

    return {
        founderToken:  founderBody.token,
        investorToken: investorBody.token,
        ideaId,
        founderId,
    };
}

// ── Main VU function ──────────────────────────────────────────────────────────
export default function (data) {
    // Each VU alternates between three realistic user journeys:
    const scenario = __VU % 3;

    if (scenario === 0) {
        // Journey A: Register a brand-new user and login
        vuRegisterAndLogin();
    } else if (scenario === 1) {
        // Journey B: Read ideas as investor (most common read path)
        vuBrowseIdeas(data.investorToken);
    } else {
        // Journey C: Make an offer as investor
        vuMakeOffer(data.investorToken, data.ideaId, data.founderId);
    }
}

// ── Journey A: register → login ───────────────────────────────────────────────
function vuRegisterAndLogin() {
    const email    = `spike_vu${__VU}_i${__ITER}_${Date.now()}@test.com`;
    const password = 'Test1234!';

    totalRequests.add(1);
    http.post(`${BASE_URL}/auth/register`,
        JSON.stringify({ email, password, role: 'INVESTOR' }), jsonParams());

    totalRequests.add(1);
    const start    = Date.now();
    const loginRes = http.post(`${BASE_URL}/auth/login`,
        JSON.stringify({ email, password }), jsonParams());
    loginDuration.add(Date.now() - start);

    const ok = check(loginRes, {
        'A: login 200': (r) => r.status === 200,
        'A: has token': (r) => {
            try { return JSON.parse(r.body).token !== undefined; } catch { return false; }
        },
    });
    errorRate.add(!ok);
    sleep(0.5);
}

// ── Journey B: browse ideas ───────────────────────────────────────────────────
function vuBrowseIdeas(investorToken) {
    if (!investorToken) return;

    totalRequests.add(1);
    const start = Date.now();
    const res   = http.get(`${BASE_URL}/ideas`, authParams(investorToken));
    ideasDuration.add(Date.now() - start);

    const ok = check(res, {
        'B: GET /ideas 200': (r) => r.status === 200,
    });
    errorRate.add(!ok);
    sleep(0.3);
}

// ── Journey C: make an offer ──────────────────────────────────────────────────
function vuMakeOffer(investorToken, ideaId, founderId) {
    if (!investorToken || !ideaId || !founderId) return;

    totalRequests.add(1);
    const start = Date.now();
    const res   = http.post(
        `${BASE_URL}/deals/offers`,
        JSON.stringify({ ideaId, founderId, amount: 5000 + __VU * 100, message: `Offer from VU ${__VU}` }),
        authParams(investorToken),
    );
    offersDuration.add(Date.now() - start);

    const ok = check(res, {
        'C: POST /deals/offers 201': (r) => r.status === 201,
    });
    errorRate.add(!ok);
    sleep(0.5);
}
