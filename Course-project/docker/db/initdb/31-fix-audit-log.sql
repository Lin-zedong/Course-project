DO $$
BEGIN

  IF to_regclass('public.audit_log') IS NULL AND to_regclass('public.audit_logs') IS NOT NULL THEN
    ALTER TABLE public.audit_logs RENAME TO audit_log;


  ELSIF to_regclass('public.audit_log') IS NULL THEN
    CREATE TABLE public.audit_log (
      id         BIGSERIAL PRIMARY KEY,
      action     VARCHAR(64) NOT NULL,
      actor      VARCHAR(64) NOT NULL,
      meta       JSONB,
      target_id  BIGINT,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON public.audit_log (created_at DESC);
