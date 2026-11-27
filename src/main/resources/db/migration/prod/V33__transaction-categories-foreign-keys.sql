-- V33: Enforce referential integrity on t_transaction_categories.
--
-- The join table had no foreign keys, so rows outlived what they point at:
--   * deleting a category (CategoryService.deleteById / deleteByCategoryNameStandardized)
--     never touched the join table, because Category is the non-owning side of the
--     Transaction.categories @ManyToMany;
--   * transactions deleted outside Hibernate left their join rows behind.
-- On 2026-11-27 production had 252 rows for missing transactions and 38 for missing
-- categories (31,743 rows in total).
--
-- Those rows reference nothing, so they are deleted. The foreign keys then make
-- PostgreSQL remove a transaction's or category's join rows whenever it is deleted,
-- whatever code path deletes it.

DELETE FROM public.t_transaction_categories tc
WHERE NOT EXISTS (SELECT 1 FROM public.t_transaction t WHERE t.transaction_id = tc.transaction_id)
   OR NOT EXISTS (SELECT 1 FROM public.t_category c WHERE c.category_id = tc.category_id);

-- The primary key is (category_id, transaction_id), which cannot serve lookups by
-- transaction_id alone; without this index every transaction delete would scan the table.
CREATE INDEX IF NOT EXISTS idx_transaction_categories_transaction_id
    ON public.t_transaction_categories (transaction_id);

ALTER TABLE public.t_transaction_categories
    ADD CONSTRAINT fk_transaction_categories_transaction
        FOREIGN KEY (transaction_id) REFERENCES public.t_transaction (transaction_id) ON DELETE CASCADE;

ALTER TABLE public.t_transaction_categories
    ADD CONSTRAINT fk_transaction_categories_category
        FOREIGN KEY (category_id) REFERENCES public.t_category (category_id) ON DELETE CASCADE;
