DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns
             WHERE table_name='events' AND column_name='eventtime') THEN
    ALTER TABLE events RENAME COLUMN eventtime TO event_time;
  END IF;

  IF EXISTS (SELECT 1 FROM information_schema.columns
             WHERE table_name='subjects' AND column_name='ruzkey') THEN
    ALTER TABLE subjects RENAME COLUMN ruzkey TO ruz_key;
  END IF;
END $$;
