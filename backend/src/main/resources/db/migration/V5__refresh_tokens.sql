-- ---------------------------------------------------------------------------
-- V5 — refresh_tokens (B004 Stage 5)
--
-- Purpose. Persist rotating refresh tokens so a customer's session can
-- outlive the 15-minute access-token TTL without asking for a password
-- again. Every login mints one; every /auth/refresh consumes and
-- reissues; every /auth/logout marks the current one revoked.
--
-- Storage model. Only the SHA-256 hash of the token is stored — the raw
-- value lives only in the HttpOnly cookie sent to the browser. A DB
-- leak therefore does not compromise sessions; the attacker would also
-- need to steal the cookie. The `token_hash` column is UNIQUE and
-- indexed so /refresh can look up a candidate in O(log n) without a
-- scan.
--
-- FK on user_id CASCADEs on delete so a hard-deleted user can't leave
-- orphan sessions behind. Disabling a user (User.enabled = false) does
-- NOT auto-revoke; the login path already refuses disabled users, and
-- /refresh loads the user and repeats the check so a disabled user's
-- refresh call returns 401 even mid-session.
--
-- revoked_at is the tombstone. Rotation sets it on the old row and
-- inserts a new one. A missing revoked_at + not-yet-expired timestamp
-- = the row is live.
--
-- Safety. New table only; empty on every environment.
-- ---------------------------------------------------------------------------

CREATE TABLE refresh_tokens (
    id          UUID                          NOT NULL PRIMARY KEY,
    user_id     UUID                          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)                   NOT NULL,
    issued_at   TIMESTAMP(6) WITH TIME ZONE   NOT NULL,
    expires_at  TIMESTAMP(6) WITH TIME ZONE   NOT NULL,
    revoked_at  TIMESTAMP(6) WITH TIME ZONE
);

CREATE UNIQUE INDEX ux_refresh_token_hash ON refresh_tokens (token_hash);
CREATE INDEX idx_refresh_token_user ON refresh_tokens (user_id);
