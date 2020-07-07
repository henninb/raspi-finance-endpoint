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
  notes VARCHAR(100),
  sha256 VARCHAR(70),
  date_updated TIMESTAMP,
  date_added TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_account(
  account_id bigint auto_increment,
  account_name_owner VARCHAR(40),
  account_name VARCHAR(20),
  account_owner VARCHAR(20),
  account_type VARCHAR(6),
  active_status boolean,
  moniker VARCHAR(4),
  totals DECIMAL(12,2),
  totals_balanced DECIMAL(12,2),
  date_closed TIMESTAMP,
  date_updated TIMESTAMP,
  date_added TIMESTAMP
);

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
