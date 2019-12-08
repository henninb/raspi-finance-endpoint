--INSERT INTO t_account(account_name_owner, account_type, active_status) VALUES('chase_brian', 'credit', 'Y');
--INSERT INTO t_transaction(guid, account_type, account_name_owner, transaction_date, description, category, amount, cleared, account_id, reoccurring) VALUES('4ea3be58-3993-46de-88a2-4ffc7f1d73bd', 'credit', 'test_brian', DATEADD('DAY',-9, CURRENT_DATE), 'Batteries Plus', 'automotive', '100.29', 1, (select account_id from t_account where account_name_owner = 'chase_brian'), false);
select * from t_account

