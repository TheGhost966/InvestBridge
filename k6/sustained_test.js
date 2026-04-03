/**
 * sustained_test.js — Steady load test to measure throughput and stability.
 *
 * Scenario:
 *   - Ramp up to 20 VUs over 30 seconds
 *   - Hold at 20 VUs for 5 minutes (sustained production-like traffic)
 *   - Ramp down to 0 over 30 seconds
 *
 * What we're testing:
 *   - Can the system handle sustained concurrent users?
 *   - Do response times stay stable over time (no memory leaks / degradation)?
 *   - Does the full business flow (register → idea → offer → accept → match) work end-to-end?
 *   - Are p95 latencies within SLO under normal load?
 *
 * Run:
 *   k6 run k6/sustained_test.js
 *   k6 run --out influxdb=http://localhost:8086/k6 k6/sustained_test.js
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { BASE_URL, jsonParams, authParams } from './helpers.js';

// ── Custom metrics ────────────────────────────────────────────────────────────
const authDuration       = new Trend('auth_flow_duration_ms', true);
const ideaCreateDuration = new Trend('idea_create_duration_ms', true);
const ideaListDuration   = new Trend('idea_list_duration_ms', true);
const offerDuration      = new Trend('offer_create_duration_ms', true);
const matchDuration      = new Trend('match_list_duration_ms', true);
const errorRate          = new Rate('business_errors');
const successfulFlows    = new Counter('successful_full_flows');

// ── Load shape ────────────────────────────────────────────────────────────────
export const options = {
    scenarios: {
        sustained_load: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 20  },  // warm up
                { duration: '5m',  target: 20  },  // sustained load
                { duration: '30s', target: 0   },  // cool down
            ],
        },
    },

    thresholds: {
        // Strict SLOs for sustained (normal) load
        http_req_failed:       ['rate<0.01'],         // < 1% HTTP errors
        http_req_duration:     ['p(95)<500', 'p(99)<1000'],  // p95 < 500ms, p99 < 1s
        checks:                ['rate>0.99'],          // 99%+ checks pass

        // Business-level SLOs
        auth_flow_duration_ms:       ['p(95)<600'],
        idea_create_duration_ms:     ['p(95)<500'],
        idea_list_duration_ms:       ['p(95)<400'],
        offer_create_duration_ms:    ['p(95)<500'],
        match_list_duration_ms:      ['p(95)<400'],
        business_errors:             ['rate<0.01'],
    },
};

// ── Setup: create shared ADMIN user to verify ideas ───────────────────────────
export function setup() {
    const ts       = Date.now();
    const password = 'Test1234!';

    const adminEmail = `sustained_admin_${ts}@test.com`;
    http.post(`${BASE_URL}/auth/register`,
        JSON.stringify({ email: adminEmail, password, role: 'ADMIN' }), jsonParams());
    const aLogin = http.post(`${BASE_URL}/auth/login`,
        JSON.stringify({ email: adminEmail, password }), jsonParams());

    return {
        adminToken: aLogin.status === 200 ? JSON.parse(aLogin.body).token : null,
        password,
    };
}

// ── Main VU function — full investment lifecycle ──────────────────────────────
export default function (data) {
    const password = data.password;
    const ts       = `${__VU}_${__ITER}_${Date.now()}`;
    let errors     = 0;

    // ── Step 1: Register FOUNDER ─────────────────────────────────────────────
    let founderToken, founderId;
    group('1. Register & login as FOUNDER', () => {
        const email = `founder_${ts}@test.com`;
        http.post(`${BASE_URL}/auth/register`,
            JSON.stringify({ email, password, role: 'FOUNDER' }), jsonParams());

        const start    = Date.now();
        const loginRes = http.post(`${BASE_URL}/auth/login`,
            JSON.stringify({ email, password }), jsonParams());
        authDuration.add(Date.now() - start);

        const ok = check(loginRes, {
            '1. founder login 200': (r) => r.status === 200,
        });
        if (!ok) { errors++; return; }
        const body  = JSON.parse(loginRes.body);
        founderToken = body.token;
        founderId    = body.userId;
    });

    if (!founderToken) { errorRate.add(1); return; }
    sleep(0.2);

    // ── Step 2: Register INVESTOR ─────────────────────────────────────────────
    let investorToken;
    group('2. Register & login as INVESTOR', () => {
        const email = `investor_${ts}@test.com`;
        http.post(`${BASE_URL}/auth/register`,
            JSON.stringify({ email, password, role: 'INVESTOR' }), jsonParams());

        const loginRes = http.post(`${BASE_URL}/auth/login`,
            JSON.stringify({ email, password }), jsonParams());

        const ok = check(loginRes, {
            '2. investor login 200': (r) => r.status === 200,
        });
        if (!ok) { errors++; return; }
        investorToken = JSON.parse(loginRes.body).token;
    });

    if (!investorToken) { errorRate.add(1); return; }
    sleep(0.2);

    // ── Step 3: FOUNDER creates an idea ──────────────────────────────────────
    let ideaId;
    group('3. FOUNDER creates idea', () => {
        const start  = Date.now();
        const ideaRes = http.post(
            `${BASE_URL}/ideas`,
            JSON.stringify({
                title:       `Idea by VU${__VU} iter${__ITER}`,
                description: 'A sustained test idea to validate end-to-end flow',
                sector:      'Technology',
                fundingGoal: 75000,
            }),
            authParams(founderToken),
        );
        ideaCreateDuration.add(Date.now() - start);

        const ok = check(ideaRes, {
            '3. POST /ideas 201': (r) => r.status === 201,
            '3. idea has id':     (r) => { try { return JSON.parse(r.body).id !== undefined; } catch { return false; } },
        });
        if (!ok) { errors++; return; }
        ideaId = JSON.parse(ideaRes.body).id;
    });

    if (!ideaId) { errorRate.add(1); return; }
    sleep(0.2);

    // ── Step 4: INVESTOR browses verified ideas ───────────────────────────────
    group('4. INVESTOR lists ideas', () => {
        const start = Date.now();
        const res   = http.get(`${BASE_URL}/ideas`, authParams(investorToken));
        ideaListDuration.add(Date.now() - start);

        const ok = check(res, {
            '4. GET /ideas 200':    (r) => r.status === 200,
            '4. paginated result':  (r) => { try { return JSON.parse(r.body).content !== undefined; } catch { return false; } },
        });
        if (!ok) errors++;
    });

    sleep(0.2);

    // ── Step 5: INVESTOR creates investor profile ─────────────────────────────
    group('5. INVESTOR creates profile', () => {
        const res = http.post(
            `${BASE_URL}/deals/profiles`,
            JSON.stringify({
                bio:           `Investor VU${__VU}`,
                sectors:       ['Technology', 'Fintech'],
                minInvestment: 5000,
                maxInvestment: 200000,
            }),
            authParams(investorToken),
        );
        // 201 on first creation, 409 if duplicate (both acceptable in sustained test)
        check(res, {
            '5. profile created or exists': (r) => r.status === 201 || r.status === 409,
        });
    });

    sleep(0.2);

    // ── Step 6: INVESTOR makes an offer ───────────────────────────────────────
    let offerId;
    group('6. INVESTOR makes offer', () => {
        const start   = Date.now();
        const offerRes = http.post(
            `${BASE_URL}/deals/offers`,
            JSON.stringify({
                ideaId,
                founderId: founderId || 'unknown',
                amount:    10000 + __VU * 500,
                message:   `Sustained test offer from VU ${__VU}`,
            }),
            authParams(investorToken),
        );
        offerDuration.add(Date.now() - start);

        const ok = check(offerRes, {
            '6. POST /deals/offers 201': (r) => r.status === 201,
        });
        if (!ok) { errors++; return; }
        offerId = JSON.parse(offerRes.body).id;
    });

    sleep(0.2);

    // ── Step 7: FOUNDER accepts the offer ─────────────────────────────────────
    group('7. FOUNDER accepts offer', () => {
        if (!offerId) return;
        const res = http.patch(
            `${BASE_URL}/deals/offers/${offerId}/accept`,
            null,
            authParams(founderToken),
        );
        check(res, {
            '7. accept offer 200': (r) => r.status === 200,
            '7. status ACCEPTED':  (r) => { try { return JSON.parse(r.body).status === 'ACCEPTED'; } catch { return false; } },
        });
    });

    sleep(0.2);

    // ── Step 8: check matches ─────────────────────────────────────────────────
    group('8. INVESTOR checks matches', () => {
        const start = Date.now();
        const res   = http.get(`${BASE_URL}/deals/matches`, authParams(investorToken));
        matchDuration.add(Date.now() - start);

        check(res, {
            '8. GET /deals/matches 200': (r) => r.status === 200,
            '8. has at least one match': (r) => { try { return JSON.parse(r.body).length >= 1; } catch { return false; } },
        });
    });

    // Track complete flow success
    if (errors === 0) successfulFlows.add(1);
    else errorRate.add(1);

    sleep(1);
}
