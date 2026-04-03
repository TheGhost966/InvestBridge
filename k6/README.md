# Load Tests — InvestBridge k6 Suite

Three test levels following the Test Pyramid for performance:

| Script | VUs | Duration | Purpose |
|--------|-----|----------|---------|
| `smoke_test.js` | 1 | ~10s | Verify all endpoints work before load testing |
| `spike_test.js` | 0 → 50 → 0 | ~50s | Test resilience under sudden traffic burst |
| `sustained_test.js` | 0 → 20 → 0 | ~6 min | Test throughput & stability under normal load |

---

## Prerequisites

**1. Start the full stack (Docker required):**
```bash
# From project root
cp .env.example .env          # set JWT_SECRET=any-32-char-string
docker-compose up --build -d  # builds all services + starts Prometheus/Grafana
docker-compose ps             # verify all containers are healthy
```

**2. Install k6:**
```bash
# Windows (winget)
winget install k6 --source winget

# macOS
brew install k6

# Or download from https://k6.io/docs/get-started/installation/
```

---

## Running the Tests

Always run smoke first — if smoke fails, fix the issue before running load tests.

### Smoke test (sanity check)
```bash
k6 run k6/smoke_test.js
```
Expected: `✓ checks: rate==1.00` — all checks green, no threshold violations.

### Spike test
```bash
k6 run k6/spike_test.js
```
Expected: error rate < 5%, p95 < 2s during the spike peak.

### Sustained load test
```bash
k6 run k6/sustained_test.js
```
Expected: error rate < 1%, p95 < 500ms, full business flow completes each iteration.

### Target a non-default host
```bash
k6 run --env BASE_URL=http://192.168.1.100:8080 k6/spike_test.js
```

---

## Reading Results

k6 prints a summary at the end:

```
✓ http_req_failed.............: 0.12%  threshold: rate<0.01  ✓ PASS
✓ http_req_duration p(95).....: 342ms  threshold: p(95)<500  ✓ PASS
✓ checks......................: 99.8%  threshold: rate>0.99  ✓ PASS
```

A `✗` means a threshold was breached — that is a performance regression.

---

## Visualizing in Grafana (Live Dashboard)

```bash
# Open Grafana (starts automatically with docker-compose)
# http://localhost:3001  —  admin / admin

# Prometheus is auto-provisioned as a datasource.
# Create a new dashboard and use these PromQL queries:

# Request rate per service
rate(http_server_requests_seconds_count{job="dispatcher"}[1m])

# p95 latency
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Dispatcher custom counter (requests by route)
rate(dispatcher_requests_total[1m])

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[1m])
  /
rate(http_server_requests_seconds_count[1m])
```

To see metrics **while k6 is running**, open Grafana in a browser tab and watch the panels update in real time.

---

## Thresholds Reference

### Spike test (`spike_test.js`)
| Metric | Threshold | Rationale |
|--------|-----------|-----------|
| `http_req_failed` | < 5% | Lenient — spike by definition stresses the system |
| `http_req_duration p(95)` | < 2000ms | System should recover, not die |
| `checks` | > 95% | Core assertions must mostly hold |
| `login_duration_ms p(95)` | < 1000ms | Auth must remain functional |

### Sustained test (`sustained_test.js`)
| Metric | Threshold | Rationale |
|--------|-----------|-----------|
| `http_req_failed` | < 1% | Normal load — errors should be near zero |
| `http_req_duration p(95)` | < 500ms | User-facing SLO |
| `http_req_duration p(99)` | < 1000ms | Tail latency bound |
| `checks` | > 99% | Near-perfect reliability expected |
| `business_errors` | < 1% | Full flows should complete |
