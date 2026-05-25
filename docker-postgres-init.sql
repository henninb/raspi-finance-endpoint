CREATE USER henninb WITH PASSWORD
SELECT 1 FROM pg_roles WHERE rolname='USR_NAME'


--echo "SELECT 'CREATE DATABASE mydb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'mydb')\gexec" | psql

-- pass the md5 hash as the password

CREATE ROLE henninb LOGIN INHERIT ENCRYPTED PASSWORD 'md53c18737e8b8d9f72677d5e66040df7bd';

"md5" + md5(password + username)
# echo -n "md5"; echo -n "password123admin" | md5sum | awk '{print $1}'

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
