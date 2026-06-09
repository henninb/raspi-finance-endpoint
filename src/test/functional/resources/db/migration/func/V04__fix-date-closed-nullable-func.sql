ALTER TABLE func.t_account ALTER COLUMN date_closed SET NULL;
UPDATE func.t_account SET date_closed = NULL WHERE date_closed = PARSEDATETIME('1970-01-01 00:00:00.0', 'yyyy-MM-dd HH:mm:ss.S');
