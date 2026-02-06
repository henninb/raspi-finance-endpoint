-- Fix owner values to match JWT username (email) instead of short username.
-- V13 backfilled owner as 'henninb' but the JWT token uses the full username
-- from t_user (e.g. 'henninb@gmail.com'), causing a mismatch at runtime.

BEGIN;

UPDATE public.t_account SET owner = 'henninb@gmail.com' WHERE owner = 'henninb';
UPDATE public.t_transaction SET owner = 'henninb@gmail.com' WHERE owner = 'henninb';
UPDATE public.t_category SET owner = 'henninb@gmail.com' WHERE owner = 'henninb';
UPDATE public.t_description SET owner = 'henninb@gmail.com' WHERE owner = 'henninb';
UPDATE public.t_payment SET owner = 'henninb@gmail.com' WHERE owner = 'henninb';
UPDATE public.t_transfer SET owner = 'henninb@gmail.com' WHERE owner = 'henninb';
UPDATE public.t_validation_amount SET owner = 'henninb@gmail.com' WHERE owner = 'henninb';
UPDATE public.t_receipt_image SET owner = 'henninb@gmail.com' WHERE owner = 'henninb';
UPDATE public.t_pending_transaction SET owner = 'henninb@gmail.com' WHERE owner = 'henninb';
UPDATE public.t_parameter SET owner = 'henninb@gmail.com' WHERE owner = 'henninb';
UPDATE public.t_transaction_categories SET owner = 'henninb@gmail.com' WHERE owner = 'henninb';
UPDATE public.t_medical_expense SET owner = 'henninb@gmail.com' WHERE owner = 'henninb';

COMMIT;
