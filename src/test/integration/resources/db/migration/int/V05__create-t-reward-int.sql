CREATE TABLE IF NOT EXISTS int.t_reward
(
    reward_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id    BIGINT           NOT NULL,
    owner         TEXT             NOT NULL,
    multiplier    NUMERIC(4, 1)    NOT NULL,
    category      TEXT             NOT NULL,
    cpp           NUMERIC(6, 4)    NOT NULL DEFAULT 0.01,
    active_status BOOLEAN          NOT NULL DEFAULT TRUE,
    date_added    TIMESTAMP        NOT NULL DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S'),
    date_updated  TIMESTAMP        NOT NULL DEFAULT PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S'),
    CONSTRAINT uk_reward_account_multiplier_category UNIQUE (account_id, multiplier, category),
    CONSTRAINT fk_reward_account_id FOREIGN KEY (account_id) REFERENCES int.t_account (account_id) ON DELETE CASCADE
);
