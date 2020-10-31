--CREATE USER henninb WITH PASSWORD 'monday1' CREATEDB;
CREATE USER henninb WITH PASSWORD 'monday1' SUPERUSER;
CREATE DATABASE finance_db;
GRANT ALL PRIVILEGES ON DATABASE finance_db TO henninb;
CREATE DATABASE finance_test_db;
GRANT ALL PRIVILEGES ON DATABASE finance_test_db TO henninb;
--CREATE DATABASE finance_db WITH OWNER = henninb;

 --   ENCODING = 'UTF8'
 --   LC_COLLATE = 'en_US.utf8'
 --   LC_CTYPE = 'en_US.utf8'
  --  TABLESPACE = pg_default
 --   CONNECTION LIMIT = -1;
