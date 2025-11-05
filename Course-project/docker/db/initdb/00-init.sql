CREATE ROLE schedule_user LOGIN PASSWORD 'schedule_password';

GRANT ALL PRIVILEGES ON DATABASE schedulewatcher TO schedule_user;
ALTER DATABASE schedulewatcher OWNER TO schedule_user;

ALTER SCHEMA public OWNER TO schedule_user;
GRANT USAGE, CREATE ON SCHEMA public TO schedule_user;

ALTER DEFAULT PRIVILEGES FOR USER postgres IN SCHEMA public GRANT ALL ON TABLES    TO schedule_user;
ALTER DEFAULT PRIVILEGES FOR USER postgres IN SCHEMA public GRANT ALL ON SEQUENCES TO schedule_user;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pgcrypto;
