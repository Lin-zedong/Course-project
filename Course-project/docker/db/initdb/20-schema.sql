SET ROLE schedule_user;
SET search_path TO public;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$ BEGIN
    CREATE TYPE subject_type AS ENUM ('group','teacher','room');
EXCEPTION WHEN duplicate_object THEN null; END $$;

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE,
    telegram_id VARCHAR(100),
    status VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE IF NOT EXISTS subjects (
    id SERIAL PRIMARY KEY,
    type varchar(16) NOT NULL,
    name VARCHAR(255) NOT NULL,
    ruz_key VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject_id INTEGER NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    channels JSONB NOT NULL DEFAULT '["web","email"]',
    filters JSONB NOT NULL DEFAULT '{}'::jsonb,
    important BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    CONSTRAINT uq_user_subject UNIQUE(user_id, subject_id)
);

CREATE TABLE IF NOT EXISTS events (
    id BIGSERIAL PRIMARY KEY,
    subject_id INTEGER NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    event_time TIMESTAMP WITH TIME ZONE NOT NULL,
    diff JSONB NOT NULL,
    hash VARCHAR(128) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    CONSTRAINT uq_subject_hash UNIQUE(subject_id, hash)
);
CREATE INDEX IF NOT EXISTS idx_events_subject_time ON events(subject_id, event_time DESC);

CREATE TABLE IF NOT EXISTS snapshots (
    id BIGSERIAL PRIMARY KEY,
    subject_id INTEGER NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    snapshot_at TIMESTAMP WITH TIME ZONE NOT NULL,
    payload_hash VARCHAR(128) NOT NULL,
    raw JSONB NOT NULL
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(50) NOT NULL,
    actor VARCHAR(50),
    target_id BIGINT,
    meta JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

RESET ROLE;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns
             WHERE table_name='subjects' AND column_name='ruz_key') THEN
ALTER TABLE subjects RENAME COLUMN ruz_key TO ruzkey;
END IF;
END$$;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns
             WHERE table_name='events' AND column_name='eventtime') THEN
ALTER TABLE events RENAME COLUMN eventtime TO event_time;
END IF;
END$$;

ALTER TABLE subscriptions
    ALTER COLUMN channels SET DEFAULT '["web","email"]'::jsonb,
ALTER COLUMN filters  SET DEFAULT '{}'::jsonb;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'subject_type') THEN
CREATE TYPE subject_type AS ENUM ('COURSE','TEACHER');
END IF;
END$$;

CREATE TABLE IF NOT EXISTS audit_log (
    id         BIGSERIAL PRIMARY KEY,
    action     VARCHAR(64) NOT NULL,
    actor      VARCHAR(64) NOT NULL,
    meta       JSONB,
    target_id  BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log (created_at DESC);
