-- ═══════════════════════════════════════════════════════════════════
--  audit_logs  —  append-only record of authentication events.
--
--  Stored in PostgreSQL (JDBC) while user identity stays in MongoDB.
--  This split satisfies the "JDBC + NoSQL in isolated layers" rule:
--  each store holds one kind of data and they never join across.
--
--  Columns
--    id         BIGSERIAL   surrogate key, auto-incrementing.
--    user_id    TEXT        Mongo ObjectId of the acting user.
--    email      TEXT        email at the moment of the event (nullable on logout).
--    event      TEXT        LOGIN | LOGOUT | REGISTER ...
--    occurred_at TIMESTAMPTZ event time in UTC (defaults to now).
--
--  Indexes
--    (user_id, occurred_at DESC)  → "last N events for user X" (common query).
--    (event, occurred_at DESC)    → "all LOGIN events today" (admin report).
-- ═══════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS audit_logs (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      TEXT         NOT NULL,
    email        TEXT,
    event        TEXT         NOT NULL,
    occurred_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_user_time
    ON audit_logs (user_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_logs_event_time
    ON audit_logs (event, occurred_at DESC);
