--TRUNCATE flyway_schema_history cascade;
TRUNCATE t_category cascade;
TRUNCATE t_description cascade;
TRUNCATE t_parm cascade;
TRUNCATE t_payment cascade;
TRUNCATE t_transaction cascade;
TRUNCATE t_transaction_categories cascade;
TRUNCATE t_receipt_image cascade;
TRUNCATE t_account cascade;

INSERT INTO t_account(account_id, account_name_owner, account_type, active_status, moniker, totals, totals_balanced, date_closed, date_updated, date_added) VALUES (1057, 'foo_brian', 'credit', true, '0000', 0.0, 0.0, '1969-12-31 18:00:00.000000', '2020-12-23 20:04:37.903600', '2020-09-05 20:33:34.077330');
INSERT INTO t_account(account_id, account_name_owner, account_type, active_status, moniker, totals, totals_balanced, date_closed, date_updated, date_added) VALUES (1058, 'bank_brian', 'debit', true, '0000', 0.0, 0.0, '1969-12-31 18:00:00.000000', '2020-12-23 20:04:37.903600', '2020-09-05 20:33:34.077330');

INSERT INTO t_category (category_id, category, active_status, date_updated, date_added) VALUES (1054, 'online', true, '1970-01-01 00:00:00.000000', '1970-01-01 00:00:00.000000');

INSERT INTO t_transaction(transaction_id, account_id, account_type, account_name_owner, guid, transaction_date, description, category, amount, transaction_state, reoccurring, reoccurring_type, active_status, notes, receipt_image_id, date_updated, date_added) VALUES (22530, 1057, 'credit', 'foo_brian', 'ba665bc2-22b6-4123-a566-6f5ab3d796de', '2020-09-04', 'current', 'online', 11.92, 'cleared', false, 'undefined', true, '', null, '2020-10-27 18:51:06.903105', '2020-09-05 20:34:39.360139');
INSERT INTO t_transaction(transaction_id, account_id, account_type, account_name_owner, guid, transaction_date, description, category, amount, transaction_state, reoccurring, reoccurring_type, active_status, notes, receipt_image_id, date_updated, date_added) VALUES (22531, 1057, 'credit', 'foo_brian', 'ba665bc2-22b6-4123-a566-6f5ab3d796df', '2020-09-05', 'current', 'online', 11.93, 'cleared', false, 'undefined', true, '', null, '2020-10-27 18:51:06.903105', '2020-09-05 20:34:39.360139');
INSERT INTO t_transaction(transaction_id, account_id, account_type, account_name_owner, guid, transaction_date, description, category, amount, transaction_state, reoccurring, reoccurring_type, active_status, notes, receipt_image_id, date_updated, date_added) VALUES (22532, 1057, 'credit', 'foo_brian', 'ba665bc2-22b6-4123-a566-6f5ab3d796dg', '2020-09-06', 'current', 'online', 11.94, 'cleared', false, 'undefined', true, '', null, '2020-10-27 18:51:06.903105', '2020-09-05 20:34:39.360139');

INSERT INTO t_parm (parm_id, parm_name, parm_value, active_status, date_updated, date_added) VALUES (1, 'payment_account', 'bank_brian', true, '2020-10-23 18:00:59.952200', '2020-10-23 18:00:59.952200');