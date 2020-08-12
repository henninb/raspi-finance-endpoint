CREATE OR REPLACE FUNCTION fn_insert_ts_transaction_categories() RETURNS TRIGGER AS
$$
BEGIN
    NEW.date_added := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;

DROP TRIGGER IF EXISTS tr_insert_ts_transaction_categories on t_transaction_categories;
CREATE TRIGGER tr_insert_ts_transaction_categories BEFORE INSERT ON t_transaction_categories FOR EACH ROW EXECUTE PROCEDURE fn_insert_ts_transaction_categories();
