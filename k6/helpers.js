/**
 * helpers.js — shared utilities for all k6 load tests
 *
 * All tests target the Dispatcher (the only public entry point).
 * Internal services are NOT reachable directly.
 */

import http from 'k6/http';
import { check } from 'k6';

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

/** Build JSON request params with correct Content-Type. */
export function jsonParams(extraHeaders = {}) {
    return {
        headers: {
            'Content-Type': 'application/json',
            ...extraHeaders,
        },
    };
}

/** Build authenticated JSON request params. */
export function authParams(token, extraHeaders = {}) {
    return jsonParams({
        Authorization: `Bearer ${token}`,
        ...extraHeaders,
    });
}

/**
 * Register a new user and return { token, userId, role }.
 * Uses __VU and __ITER to guarantee unique emails per virtual user / iteration.
 */
export function registerAndLogin(role = 'FOUNDER', suffix = '') {
    const email    = `user_${role.toLowerCase()}_vu${__VU}_iter${__ITER}${suffix}@test.com`;
    const password = 'Test1234!';

    // Register
    const regRes = http.post(
        `${BASE_URL}/auth/register`,
        JSON.stringify({ email, password, role }),
        jsonParams(),
    );
    check(regRes, { 'register 201': (r) => r.status === 201 });

    // Login
    const loginRes = http.post(
        `${BASE_URL}/auth/login`,
        JSON.stringify({ email, password }),
        jsonParams(),
    );
    check(loginRes, { 'login 200': (r) => r.status === 200 });

    if (loginRes.status !== 200) return null;

    const body = JSON.parse(loginRes.body);
    return { token: body.token, email };
}

/**
 * Register users once in setup() and return credentials for VUs to share.
 * Creates one FOUNDER and one INVESTOR.
 */
export function setupUsers() {
    const ts = Date.now();

    const founderEmail  = `founder_setup_${ts}@test.com`;
    const investorEmail = `investor_setup_${ts}@test.com`;
    const password      = 'Test1234!';

    http.post(`${BASE_URL}/auth/register`,
        JSON.stringify({ email: founderEmail,  password, role: 'FOUNDER'  }), jsonParams());
    http.post(`${BASE_URL}/auth/register`,
        JSON.stringify({ email: investorEmail, password, role: 'INVESTOR' }), jsonParams());

    const founderLogin  = http.post(`${BASE_URL}/auth/login`,
        JSON.stringify({ email: founderEmail,  password }), jsonParams());
    const investorLogin = http.post(`${BASE_URL}/auth/login`,
        JSON.stringify({ email: investorEmail, password }), jsonParams());

    return {
        founderToken:  JSON.parse(founderLogin.body).token,
        investorToken: JSON.parse(investorLogin.body).token,
    };
}
