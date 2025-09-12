-- Schema creation for @DataJpaTest which doesn't run Flyway migrations
-- This ensures the 'func' schema exists for Hibernate's default_schema configuration
CREATE SCHEMA IF NOT EXISTS func;