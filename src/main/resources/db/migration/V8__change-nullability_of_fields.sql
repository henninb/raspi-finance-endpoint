alter table t_transaction alter column date_updated set not null;
alter table t_transaction alter column date_added set not null;
alter table t_transaction alter column account_id set not null;
alter table t_transaction alter column category set not null;
alter table t_transaction alter column cleared set not null;