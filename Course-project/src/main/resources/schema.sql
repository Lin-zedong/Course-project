-- PostgreSQL schema for Schedule Watcher
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$ BEGIN
    CREATE TYPE subject_type AS ENUM ('group','teacher','room');
EXCEPTION WHEN duplicate_object THEN null; END $$;

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE,
    telegram_id VARCHAR(100),
    status VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE IF NOT EXISTS subjects (
    id SERIAL PRIMARY KEY,
    type subject_type NOT NULL,
    name VARCHAR(255) NOT NULL,
    ruzKey VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
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
    eventTime TIMESTAMP WITH TIME ZONE NOT NULL,
    diff JSONB NOT NULL,
    hash VARCHAR(128) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    CONSTRAINT uq_subject_hash UNIQUE(subject_id, hash)
);
CREATE INDEX IF NOT EXISTS idx_events_subject_time ON events(subject_id, eventTime DESC);

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
    target_id UUID,
    meta JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);
