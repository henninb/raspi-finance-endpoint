CREATE TABLE IF NOT EXISTS public.t_reward
(
    reward_id     BIGSERIAL PRIMARY KEY,
    account_id    BIGINT           NOT NULL REFERENCES public.t_account (account_id),
    owner         TEXT             NOT NULL,
    multiplier    NUMERIC(4, 1)    NOT NULL,
    category      TEXT             NOT NULL,
    cpp           NUMERIC(6, 4)    NOT NULL DEFAULT 0.01,
    active_status BOOLEAN          NOT NULL DEFAULT TRUE,
    date_added    TIMESTAMP        NOT NULL DEFAULT now(),
    date_updated  TIMESTAMP        NOT NULL DEFAULT now(),
    CONSTRAINT uk_reward_account_multiplier_category UNIQUE (account_id, multiplier, category)
);

CREATE INDEX idx_reward_owner_account ON public.t_reward (owner, account_id);
