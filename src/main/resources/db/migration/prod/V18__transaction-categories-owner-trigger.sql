-- Fix t_transaction_categories owner: Hibernate's @ManyToMany @JoinTable only
-- inserts (transaction_id, category_id), leaving owner NULL.
-- Update the existing BEFORE INSERT trigger to auto-populate owner from t_transaction.

CREATE OR REPLACE FUNCTION fn_insert_transaction_categories()
    RETURNS TRIGGER
    SET SCHEMA 'public'
    LANGUAGE PLPGSQL
AS
$$
    BEGIN
      NEW.owner := (SELECT owner FROM t_transaction WHERE transaction_id = NEW.transaction_id);
      NEW.date_updated := CURRENT_TIMESTAMP;
      NEW.date_added := CURRENT_TIMESTAMP;
      RETURN NEW;
    END;
$$;
