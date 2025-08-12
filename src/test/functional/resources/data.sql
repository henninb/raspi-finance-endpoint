DELETE FROM func.t_category;
DELETE FROM func.t_description;
DELETE FROM func.t_parameter;
DELETE FROM func.t_payment;
DELETE FROM func.t_transfer;
DELETE FROM func.t_transaction;
DELETE FROM func.t_transaction_categories;
DELETE FROM func.t_receipt_image;
DELETE FROM func.t_account;

INSERT INTO func.t_account(account_name_owner, account_type, active_status, moniker,
                      date_closed, date_updated, date_added)
VALUES ('foo_brian', 'credit', true, '0000', '1969-12-31 18:00:00.000000', '2020-12-23 20:04:37.903600',
        '2020-09-05 20:33:34.077330');
INSERT INTO func.t_account(account_name_owner, account_type, active_status, moniker,
                      date_closed, date_updated, date_added)
VALUES ('bank_brian', 'debit', true, '0000', '1969-12-31 18:00:00.000000', '2020-12-23 20:04:37.903600',
        '2020-09-05 20:33:34.077330');
INSERT INTO func.t_account(account_name_owner, account_type, active_status, moniker,
                      date_closed, date_updated, date_added)
VALUES ('referenced_brian', 'credit', true, '0000', '1969-12-31 18:00:00.000000',
        '2020-12-23 20:04:37.903600', '2020-09-05 20:33:34.077330');
INSERT INTO func.t_account(account_name_owner, account_type, active_status, moniker,
                      date_closed, date_updated, date_added)
VALUES ('delete-test_brian', 'credit', true, '0000', '1969-12-31 18:00:00.000000',
        '2020-12-23 20:04:37.903600', '2020-09-05 20:33:34.077330');
INSERT INTO func.t_account(account_name_owner, account_type, active_status, moniker,
                      date_closed, date_updated, date_added)
VALUES ('delete-me_brian', 'credit', true, '0000', '1969-12-31 18:00:00.000000',
        '2020-12-23 20:04:37.903600', '2020-09-05 20:33:34.077330');

INSERT INTO func.t_category (category_name, active_status, date_updated, date_added)
VALUES ('online', true, '1970-01-01 00:00:00.000000', '1970-01-01 00:00:00.000000');

INSERT INTO func.t_transaction(account_id, account_type, account_name_owner, guid, transaction_date,
                          description, category, amount, transaction_state, reoccurring_type,
                          active_status, notes, receipt_image_id, date_updated, date_added)
VALUES ((select account_id from func.t_account where account_name_owner='foo_brian'), 'credit', 'foo_brian', 'aaaaaaaa-bbbb-cccc-dddd-1234567890de', '2020-09-04', 'current', 'online',
        11.92, 'cleared', 'undefined', true, '', null, '2020-10-27 18:51:06.903105',
        '2020-09-05 20:34:39.360139');

INSERT INTO func.t_transaction(account_id, account_type, account_name_owner, guid, transaction_date,
                          description, category, amount, transaction_state, reoccurring_type,
                          active_status, notes, receipt_image_id, date_updated, date_added)
VALUES ((select account_id from func.t_account where account_name_owner='foo_brian'), 'credit', 'foo_brian', 'aaaaaaaa-bbbb-cccc-dddd-1234567890ef', '2020-09-05', 'current', 'online',
        11.93, 'cleared', 'undefined', true, '', null, '2020-10-27 18:51:06.903105',
        '2020-09-05 20:34:39.360139');

INSERT INTO func.t_transaction(account_id, account_type, account_name_owner, guid, transaction_date,
                          description, category, amount, transaction_state, reoccurring_type,
                          active_status, notes, receipt_image_id, date_updated, date_added)
VALUES ((select account_id from func.t_account where account_name_owner='foo_brian'), 'credit', 'foo_brian', 'ba665bc2-22b6-4123-a566-6f5ab3d796dg', '2020-09-06', 'current', 'online',
        11.94, 'cleared', 'undefined', true, '', null, '2020-10-27 18:51:06.903105',
        '2020-09-05 20:34:39.360139');

INSERT INTO func.t_transaction(account_id, account_type, account_name_owner, guid, transaction_date,
                          description, category, amount, transaction_state, reoccurring_type,
                          active_status, notes, receipt_image_id, date_updated, date_added)
VALUES ((select account_id from func.t_account where account_name_owner='referenced_brian'), 'credit', 'referenced_brian', 'ba665bc2-22b6-4123-a566-6f5ab3d796dh', '2020-12-31', 'current',
        'online', 11.95, 'cleared', 'undefined', true, '', null, '2020-10-27 18:51:06.903105',
        '2020-09-05 20:34:39.360139');

INSERT INTO func.t_transaction(account_id, account_type, account_name_owner, guid, transaction_date,
                          description, category, amount, transaction_state, reoccurring_type,
                          active_status, notes, receipt_image_id, date_updated, date_added)
VALUES ((select account_id from func.t_account where account_name_owner='bank_brian'), 'debit', 'bank_brian', 'ba665bc2-22b6-4123-a566-6f5ab3d796di', '2020-12-31', 'current', 'online',
        11.95, 'cleared', 'undefined', true, '', null, '2020-10-27 18:51:06.903105',
        '2020-09-05 20:34:39.360139');

INSERT INTO func.t_payment (account_name_owner, source_account, destination_account, transaction_date, amount, guid_source, guid_destination,
                       active_status, date_updated, date_added)
VALUES ('referenced_brian', 'referenced_brian', 'bank_brian', '2020-12-31', 11.95, 'ba665bc2-22b6-4123-a566-6f5ab3d796dh',
        'ba665bc2-22b6-4123-a566-6f5ab3d796di', true, '2021-01-09 14:26:26.739000', '2021-01-09 14:26:26.739000');

INSERT INTO func.t_parameter (parameter_name, parameter_value, active_status, date_updated, date_added)
VALUES ('paymenfunc.t_account', 'bank_brian', true, '2020-10-23 18:00:59.952200', '2020-10-23 18:00:59.952200');
