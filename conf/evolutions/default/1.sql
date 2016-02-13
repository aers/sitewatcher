# --- !Ups

CREATE TABLE sites (
  id           BIGSERIAL PRIMARY KEY,
  "alias"      TEXT,
  url          TEXT,
  last_updated TIMESTAMPTZ,
  last_hash    TEXT
);

CREATE TABLE update_history (
  id               BIGSERIAL PRIMARY KEY,
  run_time         TIMESTAMPTZ,
  successful_count INT,
  update_count     INT,
  failed_count     INT
);

CREATE TABLE update_error_log (
  id           BIGSERIAL PRIMARY KEY,
  site_id      BIGSERIAL REFERENCES sites (id) ON DELETE CASCADE,
  update_id    BIGSERIAL REFERENCES update_history (id) ON DELETE CASCADE,
  error_reason TEXT,
  "time"       TIMESTAMPTZ
);

CREATE TABLE site_update_history (
  id        BIGSERIAL PRIMARY KEY,
  site_id   BIGSERIAL REFERENCES sites (id) ON DELETE CASCADE,
  update_id BIGSERIAL REFERENCES update_history (id) ON DELETE CASCADE,
  "hash"    TEXT,
  full_text TEXT,
  "time"    TIMESTAMPTZ
);

# --- !Downs

DROP TABLE IF EXISTS site_update_history;
DROP TABLE IF EXISTS update_error_log;
DROP TABLE IF EXISTS update_history;
DROP TABLE IF EXISTS sites;
