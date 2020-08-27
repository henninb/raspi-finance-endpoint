CREATE TABLE IF NOT EXISTS t_payment(
    payment_id BIGINT DEFAULT nextval('t_payment_payment_id_seq') NOT NULL,
    account_name_owner VARCHAR(40) NOT NULL,
    transaction_date DATE NOT NULL,
    amount DECIMAL(12,2) NOT NULL DEFAULT 0.0
);

CREATE TABLE IF NOT EXISTS t_transaction (
  transaction_id bigint auto_increment,
  account_id bigint,
  account_type VARCHAR(6),
  account_name_owner VARCHAR(40),
  guid VARCHAR(36) NOT NULL,
  transaction_date DATE,
  description VARCHAR(75),
  category VARCHAR(50),
  amount DECIMAL(12,2),
  cleared INTEGER,
  reoccurring BOOLEAN,
  notes TEXT,
  date_updated TIMESTAMP,
  date_added TIMESTAMP
);
-- drop table t_account;
CREATE TABLE IF NOT EXISTS t_account(
  account_id bigint auto_increment,
  account_name_owner TEXT NOT NULL,
  account_name TEXT,
  account_owner TEXT,
  account_type TEXT NOT NULL DEFAULT 'unknown',
  active_status boolean NOT NULL DEFAULT TRUE,
  moniker TEXT DEFAULT '0000',
  totals DECIMAL(12,2) DEFAULT 0.0,
  totals_balanced DECIMAL(12,2) DEFAULT 0.0,
  date_closed TIMESTAMP,
  date_updated TIMESTAMP,
  date_added TIMESTAMP,
  CONSTRAINT pk_account_id PRIMARY KEY (account_id),
--   CONSTRAINT unique_account_name_owner_account_id UNIQUE (account_id, account_name_owner, account_type),
  CONSTRAINT t_account_account_name_owner_lowercase_ck CHECK (account_name_owner = lower(account_name_owner)),
  CONSTRAINT t_account_account_type_lowercase_ck CHECK (account_type = lower(account_type))
);

-- ALTER TABLE t_account ADD CONSTRAINT TEST_UNIQUE UNIQUE ( account_id, account_name_owner, account_type);
--insert into t_account(account_name_owner, account_type, active_status,date_added, date_updated) VALUES('test_brian', 'credit', true, now(), now());

-- ALTER TABLE t_account ADD CONSTRAINT unique_account_name_owner_account_id UNIQUE(account_id, account_name_owner, account_type);

-- CREATE TABLE IF NOT EXISTS t_account(
--     account_id BIGINT auto_increment,
--     account_name_owner VARCHAR(40) NOT NULL,
--     account_name VARCHAR(40), -- NULL for now
--     account_owner VARCHAR(40), -- NULL for now
--     account_type VARCHAR(40) NOT NULL,
--     active_status BOOLEAN NOT NULL DEFAULT TRUE,
--     moniker VARCHAR(40) NOT NULL DEFAULT '',
--     totals DECIMAL(12,2) DEFAULT 0.0,
--     totals_balanced DECIMAL(12,2) DEFAULT 0.0,
--     date_closed TIMESTAMP DEFAULT TO_TIMESTAMP(0),
--     date_updated TIMESTAMP DEFAULT TO_TIMESTAMP(0),
--     date_added TIMESTAMP DEFAULT TO_TIMESTAMP(0)
-- --     CONSTRAINT pk_account_id PRIMARY KEY (account_id),
-- --     CONSTRAINT unique_account_name_owner_account_id UNIQUE (account_id, account_name_owner, account_type),
-- --     CONSTRAINT t_account_account_name_owner_lowercase_ck CHECK (account_name_owner = lower(account_name_owner)),
-- --     CONSTRAINT t_account_account_type_lowercase_ck CHECK (account_type = lower(account_type))
-- );

CREATE TABLE IF NOT EXISTS t_category(
  category_id bigint auto_increment,
  category VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS t_transaction_categories(
  category_id BIGINT NOT NULL,
  transaction_id BIGINT NOT NULL,
  date_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  date_added TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
