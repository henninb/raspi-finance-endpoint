package finance.helpers

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Slf4j
@Component
class TestDataManager {

    @Autowired
    JdbcTemplate jdbcTemplate

    private static String cleanOwner(String testOwner) {
        String owner = testOwner.replaceAll(/[^a-z]/, '').toLowerCase()
        return owner.isEmpty() ? "testowner" : owner
    }

    private static String accountNameFor(String testOwner, String suffix) {
        String cleanSuffix = suffix.replaceAll(/[^a-z-]/, '').toLowerCase()
        if (cleanSuffix.isEmpty()) cleanSuffix = "account"
        return "${cleanSuffix}_${cleanOwner(testOwner)}".toLowerCase()
    }

    void initializeIntegrationTestEnvironment(String testOwner) {
        log.info("Initializing integration test environment for owner: ${testOwner}")

        // Clean up any existing data for this test owner first
        cleanupIntegrationTestsFor(testOwner)

        // Create minimal required reference data for integration tests
        createMinimalCategoriesFor(testOwner)
        createMinimalAccountsFor(testOwner)

        log.info("Successfully initialized integration test environment for owner: ${testOwner}")
    }

    void createMinimalAccountsFor(String testOwner) {
        log.info("Creating minimal account data for integration test owner: ${testOwner}")

        // Generate pattern-compliant account names for ALPHA_UNDERSCORE_PATTERN: ^[a-z-]*_[a-z]*$
        String ownerClean = cleanOwner(testOwner)

        // Ensure primary and secondary accounts exist (idempotent)
        ensureAccountExists(testOwner, "primary")
        ensureAccountExists(testOwner, "secondary")

        log.info("Successfully created minimal account data for integration test owner: ${testOwner}")
    }

    void createMinimalCategoriesFor(String testOwner) {
        log.info("Creating minimal category data for integration test owner: ${testOwner}")

        // Create basic category needed for transactions
        String categoryName = "test_category_${cleanOwner(testOwner)}".toLowerCase()
        try {
            jdbcTemplate.update("""
                INSERT INTO t_category (category_name, active_status, owner, date_updated, date_added)
                VALUES (?, true, ?, '1970-01-01 00:00:00.000000', '1970-01-01 00:00:00.000000')
            """, categoryName, testOwner)
        } catch (Exception e) {
            log.warn("Failed to create category ${categoryName} for integration test, possibly already exists: ${e.message}")
        }

        log.info("Successfully created minimal category data for integration test owner: ${testOwner}")
    }

    String createAccountFor(String testOwner, String accountSuffix, String accountType = 'credit', boolean activeStatus = true) {
        // Generate pattern-compliant account names for ALPHA_UNDERSCORE_PATTERN: ^[a-z-]*_[a-z]*$
        String accountName = accountNameFor(testOwner, accountSuffix)

        try {
            jdbcTemplate.update("""
                INSERT INTO t_account(account_name_owner, account_type, active_status, moniker, owner,
                                  date_closed, date_updated, date_added, validation_date, future, outstanding, cleared)
                VALUES (?, ?, ?, '0000', ?, '1969-12-31 18:00:00.000000',
                        '2020-12-23 20:04:37.903600', '2020-09-05 20:33:34.077330',
                        '2020-09-05 20:33:34.077330', 0.00, 0.00, 0.00)
            """, accountName, accountType, activeStatus, testOwner)
        } catch (Exception e) {
            // Account might already exist due to race conditions or incomplete cleanup
            log.warn("Failed to create account ${accountName} for integration test, possibly already exists: ${e.message}")
        }

        log.info("Created account: ${accountName} (${accountType}) for integration test owner: ${testOwner}")
        return accountName
    }

    Long createAccountForIntegrationTest(String testOwner, String accountSuffix, String accountType) {
        String accountName = createAccountFor(testOwner, accountSuffix, accountType, true)

        // Get the generated account ID
        Long accountId = jdbcTemplate.queryForObject(
            "SELECT account_id FROM t_account WHERE account_name_owner = ?",
            Long.class, accountName
        )

        log.info("Created account for integration test: ID=${accountId}, name=${accountName}, type=${accountType}")
        return accountId
    }

    String createCategoryFor(String testOwner, String categorySuffix, boolean activeStatus = true) {
        // ALPHA_NUMERIC_NO_SPACE_PATTERN allows underscores and dashes: "^[a-z0-9_-]*$"
        String ownerPart = testOwner.replaceAll(/[^a-z0-9]/, '')
        if (ownerPart.isEmpty()) ownerPart = "test"
        String categoryName = "${categorySuffix}_${ownerPart}".toLowerCase()

        try {
            jdbcTemplate.update("""
                INSERT INTO t_category (category_name, active_status, owner, date_updated, date_added)
                VALUES (?, ?, ?, '1970-01-01 00:00:00.000000', '1970-01-01 00:00:00.000000')
            """, categoryName, activeStatus, testOwner)
        } catch (Exception e) {
            // Category might already exist due to race conditions or incomplete cleanup
            log.warn("Failed to create category ${categoryName} for integration test, possibly already exists: ${e.message}")
        }

        log.info("Created category: ${categoryName} (active: ${activeStatus}) for integration test owner: ${testOwner}")
        return categoryName
    }

    void createCategoriesFor(String testOwner, List<String> categoryNames) {
        categoryNames.each { suffix ->
            createCategoryFor(testOwner, suffix, true)
        }
        log.info("Created ${categoryNames.size()} categories for integration test owner: ${testOwner}")
    }

    String createParameterFor(String testOwner, String parameterNameSuffix, String parameterValueSuffix, boolean activeStatus = true) {
        String parameterName = "${parameterNameSuffix}_${testOwner}".toLowerCase()
        String parameterValue = "${parameterValueSuffix}_${testOwner}".toLowerCase()

        // Ensure length constraints (1-50 chars)
        if (parameterName.length() > 50) {
            String shortOwner = testOwner.length() > 8 ? testOwner[0..7] : testOwner
            parameterName = "${parameterNameSuffix}_${shortOwner}".toLowerCase()
        }

        if (parameterValue.length() > 50) {
            String shortOwner = testOwner.length() > 8 ? testOwner[0..7] : testOwner
            parameterValue = "${parameterValueSuffix}_${shortOwner}".toLowerCase()
        }

        jdbcTemplate.update("""
            INSERT INTO t_parameter (parameter_name, parameter_value, active_status, owner, date_updated, date_added)
            VALUES (?, ?, ?, ?, '1970-01-01 00:00:00.000000', '1970-01-01 00:00:00.000000')
        """, parameterName, parameterValue, activeStatus, testOwner)

        log.info("Created parameter: ${parameterName}=${parameterValue} (active: ${activeStatus}) for integration test owner: ${testOwner}")
        return parameterName
    }

    String createDescriptionFor(String testOwner, String descriptionSuffix, boolean activeStatus = true) {
        String descriptionName = "${descriptionSuffix}_${testOwner}".toLowerCase()

        // Ensure length constraints (1-50 chars)
        if (descriptionName.length() > 50) {
            String shortOwner = testOwner.length() > 8 ? testOwner[0..7] : testOwner
            descriptionName = "${descriptionSuffix}_${shortOwner}".toLowerCase()
        }

        jdbcTemplate.update("""
            INSERT INTO t_description (description_name, active_status, owner, date_updated, date_added)
            VALUES (?, ?, ?, '1970-01-01 00:00:00.000000', '1970-01-01 00:00:00.000000')
        """, descriptionName, activeStatus, testOwner)

        log.info("Created description: ${descriptionName} (active: ${activeStatus}) for integration test owner: ${testOwner}")
        return descriptionName
    }

    String createTransactionFor(String testOwner, String accountSuffix = "primary",
                                String description = "integration_test_transaction",
                                BigDecimal amount = new BigDecimal("10.00"),
                                String transactionState = "cleared") {

        String accountName = accountNameFor(testOwner, accountSuffix)
        String guid = UUID.randomUUID().toString()
        String categoryName = "test_category_${cleanOwner(testOwner)}".toLowerCase()

        // Ensure account exists
        ensureAccountExists(testOwner, accountSuffix)

        // Ensure category exists
        ensureCategoryExists(testOwner, "test_category")

        jdbcTemplate.update("""
            INSERT INTO t_transaction(account_id, account_type, account_name_owner, guid, transaction_date,
                                  description, category, amount, transaction_state, reoccurring_type,
                                  active_status, notes, receipt_image_id, owner, date_updated, date_added)
            VALUES ((select account_id from t_account where account_name_owner=?),
                    (select account_type from t_account where account_name_owner=?),
                    ?, ?, '2020-09-04', ?, ?, ?, ?, 'undefined', true, '', null, ?,
                    '2020-10-27 18:51:06.903105', '2020-09-05 20:34:39.360139')
        """, accountName, accountName, accountName, guid, description,
             categoryName, amount, transactionState, testOwner)

        log.info("Created integration test transaction ${guid} for ${accountName} with amount ${amount}")
        return guid
    }

    private void ensureAccountExists(String testOwner, String accountSuffix) {
        String accountName = "${accountSuffix}_${testOwner}".toLowerCase()
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM t_account WHERE account_name_owner = ? AND owner = ?",
            Integer.class, accountName, testOwner
        )

        if (count == 0) {
            log.info("Account ${accountName} doesn't exist, creating it for integration test")
            createAccountFor(testOwner, accountSuffix, 'credit', true)
        }
    }

    private void ensureCategoryExists(String testOwner, String categorySuffix) {
        String categoryName = "${categorySuffix}_${cleanOwner(testOwner)}".toLowerCase().replaceAll(/[^a-zA-Z0-9]/, '')
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM t_category WHERE category_name = ? AND owner = ?",
            Integer.class, categoryName, testOwner
        )

        if (count == 0) {
            log.info("Category ${categoryName} doesn't exist, creating it for integration test")
            createCategoryFor(testOwner, categorySuffix, true)
        }
    }

    Long createValidationAmountFor(String testOwner, BigDecimal amount = new BigDecimal("100.00"), String transactionState = "cleared") {
        // Find the primary account for this test owner
        String accountName = accountNameFor(testOwner, 'primary')
        Long accountId = jdbcTemplate.queryForObject(
            "SELECT account_id FROM t_account WHERE account_name_owner = ?",
            Long.class, accountName
        )

        // Insert validation amount
        jdbcTemplate.update("""
            INSERT INTO t_validation_amount (account_id, validation_date, active_status, transaction_state, amount, owner, date_updated, date_added)
            VALUES (?, NOW(), true, ?, ?, ?, '1970-01-01 00:00:00.000000', '1970-01-01 00:00:00.000000')
        """, accountId, transactionState, amount, testOwner)

        // Get the generated validation_id
        Long validationId = jdbcTemplate.queryForObject(
            "SELECT LAST_INSERT_ID()",
            Long.class
        )

        log.info("Created validation amount for integration test: ID=${validationId}, accountId=${accountId}, amount=${amount}, state=${transactionState} for test owner: ${testOwner}")
        return validationId
    }

    void cleanupIntegrationTestsFor(String testOwner) {
        log.info("Cleaning up integration test data for owner: ${testOwner}")

        // Delete in proper order to respect foreign key constraints

        // Delete payments first
        String clean = cleanOwner(testOwner)
        jdbcTemplate.update("DELETE FROM t_payment WHERE source_account LIKE ? OR destination_account LIKE ?",
                           "%${testOwner}", "%${testOwner}")
        jdbcTemplate.update("DELETE FROM t_payment WHERE source_account LIKE ? OR destination_account LIKE ?",
                           "%${clean}", "%${clean}")

        // Delete transaction categories
        jdbcTemplate.update("DELETE FROM t_transaction_categories WHERE transaction_id IN " +
                           "(SELECT transaction_id FROM t_transaction WHERE account_name_owner LIKE ?)",
                           "%${testOwner}")
        jdbcTemplate.update("DELETE FROM t_transaction_categories WHERE transaction_id IN " +
                           "(SELECT transaction_id FROM t_transaction WHERE account_name_owner LIKE ?)",
                           "%${clean}")

        // Delete transactions
        jdbcTemplate.update("DELETE FROM t_transaction WHERE account_name_owner LIKE ?", "%${testOwner}")
        jdbcTemplate.update("DELETE FROM t_transaction WHERE account_name_owner LIKE ?", "%${clean}")

        // Delete pending transactions (FK constraint requires this before deleting accounts)
        jdbcTemplate.update("DELETE FROM t_pending_transaction WHERE account_name_owner LIKE ?", "%${testOwner}")
        jdbcTemplate.update("DELETE FROM t_pending_transaction WHERE account_name_owner LIKE ?", "%${clean}")

        // Delete test-specific validation amounts (FK constraint requires this before deleting accounts)
        jdbcTemplate.update("DELETE FROM t_validation_amount WHERE account_id IN " +
                           "(SELECT account_id FROM t_account WHERE account_name_owner LIKE ?)", "%${testOwner}")
        jdbcTemplate.update("DELETE FROM t_validation_amount WHERE account_id IN " +
                           "(SELECT account_id FROM t_account WHERE account_name_owner LIKE ?)", "%${clean}")

        // Delete accounts (with error handling for FK constraints)
        try {
            jdbcTemplate.update("DELETE FROM t_account WHERE account_name_owner LIKE ?", "%${testOwner}")
        } catch (Exception e) {
            log.warn("Failed to delete accounts for integration test ${testOwner}, possibly due to remaining FK references: ${e.message}")
        }
        try {
            jdbcTemplate.update("DELETE FROM t_account WHERE account_name_owner LIKE ?", "%${clean}")
        } catch (Exception e) {
            log.warn("Failed to delete accounts for integration test ${clean}, possibly due to remaining FK references: ${e.message}")
        }

        // Delete test-specific categories
        jdbcTemplate.update("DELETE FROM t_category WHERE category_name LIKE ?", "%${testOwner}")
        jdbcTemplate.update("DELETE FROM t_category WHERE category_name LIKE ?", "%${clean}")

        // Delete test-specific parameters
        jdbcTemplate.update("DELETE FROM t_parameter WHERE parameter_name LIKE ?", "%${testOwner}")
        jdbcTemplate.update("DELETE FROM t_parameter WHERE parameter_name LIKE ?", "%${clean}")

        // Delete test-specific descriptions
        jdbcTemplate.update("DELETE FROM t_description WHERE description_name LIKE ?", "%${testOwner}")
        jdbcTemplate.update("DELETE FROM t_description WHERE description_name LIKE ?", "%${clean}")

        // Delete test-specific users
        jdbcTemplate.update("DELETE FROM t_user WHERE username LIKE ?", "%${testOwner}")
        jdbcTemplate.update("DELETE FROM t_user WHERE username LIKE ?", "%${clean}")

        log.info("Successfully cleaned up integration test data for owner: ${testOwner}")
    }

    void cleanupAllIntegrationTestData() {
        log.info("Performing full integration test data cleanup")

        // Clean up any test data that might have been left behind
        jdbcTemplate.update("DELETE FROM t_payment WHERE source_account LIKE 'test_%' OR destination_account LIKE 'test_%'")
        jdbcTemplate.update("DELETE FROM t_payment WHERE source_account LIKE 'primary_%' OR destination_account LIKE 'primary_%'")
        jdbcTemplate.update("DELETE FROM t_payment WHERE source_account LIKE 'secondary_%' OR destination_account LIKE 'secondary_%'")

        jdbcTemplate.update("DELETE FROM t_transaction_categories WHERE transaction_id IN " +
                           "(SELECT transaction_id FROM t_transaction WHERE account_name_owner LIKE 'test_%')")
        jdbcTemplate.update("DELETE FROM t_transaction_categories WHERE transaction_id IN " +
                           "(SELECT transaction_id FROM t_transaction WHERE account_name_owner LIKE 'primary_%')")
        jdbcTemplate.update("DELETE FROM t_transaction_categories WHERE transaction_id IN " +
                           "(SELECT transaction_id FROM t_transaction WHERE account_name_owner LIKE 'secondary_%')")

        jdbcTemplate.update("DELETE FROM t_transaction WHERE account_name_owner LIKE 'test_%'")
        jdbcTemplate.update("DELETE FROM t_transaction WHERE account_name_owner LIKE 'primary_%'")
        jdbcTemplate.update("DELETE FROM t_transaction WHERE account_name_owner LIKE 'secondary_%'")

        jdbcTemplate.update("DELETE FROM t_account WHERE account_name_owner LIKE 'test_%'")
        jdbcTemplate.update("DELETE FROM t_account WHERE account_name_owner LIKE 'primary_%'")
        jdbcTemplate.update("DELETE FROM t_account WHERE account_name_owner LIKE 'secondary_%'")

        jdbcTemplate.update("DELETE FROM t_category WHERE category_name LIKE 'test_%'")
        jdbcTemplate.update("DELETE FROM t_parameter WHERE parameter_name LIKE 'test_%'")
        jdbcTemplate.update("DELETE FROM t_description WHERE description_name LIKE 'test_%'")
        jdbcTemplate.update("DELETE FROM t_user WHERE username LIKE 'test_%'")

        // Clean up validation amounts
        jdbcTemplate.update("DELETE FROM t_validation_amount WHERE account_id IN " +
                           "(SELECT account_id FROM t_account WHERE account_name_owner LIKE 'test_%')")

        log.info("Completed full integration test data cleanup")
    }
}