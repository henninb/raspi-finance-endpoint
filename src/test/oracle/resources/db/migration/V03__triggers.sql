-- t_account
CREATE OR REPLACE TRIGGER tr_update_account
AFTER INSERT
      ON t_account
          FOR EACH ROW
BEGIN
UPDATE t_account(date_updated) VALUES(sysdate);
END;

-- t_transaction_categories
CREATE OR REPLACE TRIGGER tr_update_transaction_categories
AFTER INSERT
      ON t_transaction_categories
          FOR EACH ROW
BEGIN
UPDATE t_transaction_categories(date_updated) VALUES(sysdate);
END;

-- t_receipt_image
CREATE OR REPLACE TRIGGER tr_update_receipt_image
AFTER INSERT
      ON t_receipt_image
          FOR EACH ROW
BEGIN
UPDATE t_receipt_image(date_updated) VALUES(sysdate);
END;

-- t_transaction
CREATE OR REPLACE TRIGGER tr_update_transaction
AFTER INSERT
      ON t_transaction
          FOR EACH ROW
BEGIN
UPDATE t_transaction(date_updated) VALUES(sysdate);
END;

-- t_payment
CREATE OR REPLACE TRIGGER tr_update_payment
AFTER INSERT
      ON t_payment
          FOR EACH ROW
BEGIN
UPDATE t_payment(date_updated) VALUES(sysdate);
END;

-- t_parm
CREATE OR REPLACE TRIGGER tr_update_parm
AFTER INSERT
      ON t_parm
          FOR EACH ROW
BEGIN
UPDATE t_parm(date_updated) VALUES(sysdate);
END;

-- t_description
CREATE OR REPLACE TRIGGER tr_update_description
AFTER INSERT
      ON t_description
          FOR EACH ROW
BEGIN
UPDATE t_description(date_updated) VALUES(sysdate);
END;
