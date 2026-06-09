-- Drop the t_pending_transaction table as the feature has been sunset.
BEGIN;
DROP TABLE IF EXISTS public.t_pending_transaction;
COMMIT;
