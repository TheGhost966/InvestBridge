/**
 * smoke_test.js — Sanity check before running real load tests.
 *
 * Goal: verify every critical endpoint is reachable and returns the
 *       expected HTTP status code. No performance thresholds.
 *
 * Run:
 *   k6 run k6/smoke_test.js
 *   k6 run --env BASE_URL=http://localhost:8080 k6/smoke_test.js
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { BASE_URL, jsonParams, authParams } from './helpers.js';

export const options = {
    vus:        1,
    iterations: 1,
    thresholds: {
        // Every single check must pass for smoke to be green
        checks: ['rate==1.00'],
    },
};

export default function () {

    // ── 1. Auth: register a FOUNDER ──────────────────────────────────────────
    let founderToken, founderId, ideaId;

    group('Auth — register + login (FOUNDER)', () => {
        const email    = `smoke_founder_${Date.now()}@test.com`;
        const password = 'Test1234!';

        const regRes = http.post(
            `${BASE_URL}/auth/register`,
            JSON.stringify({ email, password, role: 'FOUNDER' }),
            jsonParams(),
        );
        check(regRes, {
            'register → 201': (r) => r.status === 201,
            'register body has id': (r) => JSON.parse(r.body).userId !== undefined,
        });

        const loginRes = http.post(
            `${BASE_URL}/auth/login`,
            JSON.stringify({ email, password }),
            jsonParams(),
        );
        check(loginRes, {
            'login → 200': (r) => r.status === 200,
            'login returns token': (r) => JSON.parse(r.body).token !== undefined,
        });

        const body   = JSON.parse(loginRes.body);
        founderToken = body.token;
        founderId    = body.userId;
    });

    sleep(0.3);

    // ── 2. Auth: register an INVESTOR ─────────────────────────────────────────
    let investorToken;

    group('Auth — register + login (INVESTOR)', () => {
        const email    = `smoke_investor_${Date.now()}@test.com`;
        const password = 'Test1234!';

        http.post(`${BASE_URL}/auth/register`,
            JSON.stringify({ email, password, role: 'INVESTOR' }), jsonParams());

        const loginRes = http.post(`${BASE_URL}/auth/login`,
            JSON.stringify({ email, password }), jsonParams());
        check(loginRes, {
            'investor login → 200': (r) => r.status === 200,
        });
        investorToken = JSON.parse(loginRes.body).token;
    });

    sleep(0.3);

    // ── 3. Auth: get own profile ──────────────────────────────────────────────
    group('Auth — GET /auth/me', () => {
        const res = http.get(`${BASE_URL}/auth/me`, authParams(founderToken));
        check(res, {
            'GET /auth/me → 200': (r) => r.status === 200,
            'profile has email':   (r) => JSON.parse(r.body).email !== undefined,
        });
    });

    sleep(0.3);

    // ── 4. Ideas: create an idea as FOUNDER ───────────────────────────────────
    group('Ideas — POST /ideas (FOUNDER)', () => {
        const res = http.post(
            `${BASE_URL}/ideas`,
            JSON.stringify({
                title:         'Smoke Test Idea',
                summary:       'An idea created during the smoke test',
                market:        'Technology',
                fundingNeeded: 50000,
            }),
            authParams(founderToken),
        );
        check(res, {
            'POST /ideas → 201': (r) => r.status === 201,
            'idea has id':       (r) => JSON.parse(r.body).id !== undefined,
        });
        if (res.status === 201) {
            ideaId = JSON.parse(res.body).id;
        }
    });

    sleep(0.3);

    // ── 5. Ideas: list verified ideas as INVESTOR ─────────────────────────────
    group('Ideas — GET /ideas (INVESTOR)', () => {
        const res = http.get(`${BASE_URL}/ideas`, authParams(investorToken));
        check(res, {
            'GET /ideas → 200':      (r) => r.status === 200,
            'ideas list is array':   (r) => Array.isArray(JSON.parse(r.body).content),
        });
    });

    sleep(0.3);

    // ── 6. Deals: create investor profile ─────────────────────────────────────
    group('Deals — POST /deals/profiles (INVESTOR)', () => {
        const res = http.post(
            `${BASE_URL}/deals/profiles`,
            JSON.stringify({
                bio:           'Smoke test investor',
                sectors:       ['Technology'],
                minInvestment: 1000,
                maxInvestment: 100000,
            }),
            authParams(investorToken),
        );
        check(res, {
            'POST /deals/profiles → 201': (r) => r.status === 201,
        });
    });

    sleep(0.3);

    // ── 7. Deals: create an offer ─────────────────────────────────────────────
    group('Deals — POST /deals/offers (INVESTOR)', () => {
        if (!ideaId || !founderId) {
            console.warn('Skipping offer test — no ideaId/founderId available');
            return;
        }
        const res = http.post(
            `${BASE_URL}/deals/offers`,
            JSON.stringify({
                ideaId,
                founderId,
                amount:  5000,
                message: 'Smoke test offer',
            }),
            authParams(investorToken),
        );
        check(res, {
            'POST /deals/offers → 201': (r) => r.status === 201,
        });
    });

    sleep(0.3);

    // ── 8. Deals: list matches ────────────────────────────────────────────────
    group('Deals — GET /deals/matches (INVESTOR)', () => {
        const res = http.get(`${BASE_URL}/deals/matches`, authParams(investorToken));
        check(res, {
            'GET /deals/matches → 200':    (r) => r.status === 200,
            'matches list is array':       (r) => Array.isArray(JSON.parse(r.body)),
        });
    });
}
