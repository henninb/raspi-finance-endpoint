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

    void createMinimalAccountsFor(String testOwner) {
        log.info("Creating minimal test data for owner: ${testOwner}")

        // Clean up any existing data for this test owner first
        cleanupAccountsFor(testOwner)

        // Generate pattern-compliant account names for ALPHA_UNDERSCORE_PATTERN: ^[a-z-]*_[a-z]*$
        String ownerClean = cleanOwner(testOwner)

        // Ensure primary and secondary accounts exist (idempotent)
        ensureAccountExists(testOwner, "primary")
        ensureAccountExists(testOwner, "secondary")

        // Create basic category needed for transactions
        String categoryName = "test_category_${ownerClean}".toLowerCase()
        jdbcTemplate.update("""
            INSERT INTO func.t_category (category_name, active_status, date_updated, date_added)
            VALUES (?, true, '1970-01-01 00:00:00.000000', '1970-01-01 00:00:00.000000')
        """, categoryName)

        log.info("Successfully created minimal test data for owner: ${testOwner}")
    }

    String createAccountFor(String testOwner, String accountSuffix, String accountType = 'credit', boolean activeStatus = true) {
        // Generate pattern-compliant account names for ALPHA_UNDERSCORE_PATTERN: ^[a-z-]*_[a-z]*$
        String accountName = accountNameFor(testOwner, accountSuffix)

        try {
            jdbcTemplate.update("""
                INSERT INTO func.t_account(account_name_owner, account_type, active_status, moniker,
                                      date_closed, date_updated, date_added)
                VALUES (?, ?, ?, '0000', '1969-12-31 18:00:00.000000',
                        '2020-12-23 20:04:37.903600', '2020-09-05 20:33:34.077330')
            """, accountName, accountType, activeStatus)
        } catch (Exception e) {
            // Account might already exist due to race conditions or incomplete cleanup
            log.warn("Failed to create account ${accountName}, possibly already exists: ${e.message}")
        }

        log.info("Created account: ${accountName} (${accountType}) for test owner: ${testOwner}")
        return accountName
    }

    String createCategoryFor(String testOwner, String categorySuffix, boolean activeStatus = true) {
        // ALPHA_NUMERIC_NO_SPACE_PATTERN allows underscores and dashes: "^[a-z0-9_-]*$"
        String ownerPart = testOwner.replaceAll(/[^a-z0-9]/, '')
        if (ownerPart.isEmpty()) ownerPart = "test"
        String categoryName = "${categorySuffix}_${ownerPart}".toLowerCase()

        try {
            jdbcTemplate.update("""
                INSERT INTO func.t_category (category_name, active_status, date_updated, date_added)
                VALUES (?, ?, '1970-01-01 00:00:00.000000', '1970-01-01 00:00:00.000000')
            """, categoryName, activeStatus)
        } catch (Exception e) {
            // Category might already exist due to race conditions or incomplete cleanup
            log.warn("Failed to create category ${categoryName}, possibly already exists: ${e.message}")
        }

        log.info("Created category: ${categoryName} (active: ${activeStatus}) for test owner: ${testOwner}")
        return categoryName
    }

    void createCategoriesFor(String testOwner, List<String> categoryNames) {
        categoryNames.each { suffix ->
            createCategoryFor(testOwner, suffix, true)
        }
        log.info("Created ${categoryNames.size()} categories for test owner: ${testOwner}")
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
            INSERT INTO func.t_parameter (parameter_name, parameter_value, active_status, date_updated, date_added)
            VALUES (?, ?, ?, '1970-01-01 00:00:00.000000', '1970-01-01 00:00:00.000000')
        """, parameterName, parameterValue, activeStatus)

        log.info("Created parameter: ${parameterName}=${parameterValue} (active: ${activeStatus}) for test owner: ${testOwner}")
        return parameterName
    }

    void createParametersFor(String testOwner, Map<String, String> parameterPairs) {
        parameterPairs.each { nameSuffix, valueSuffix ->
            createParameterFor(testOwner, nameSuffix, valueSuffix, true)
        }
        log.info("Created ${parameterPairs.size()} parameters for test owner: ${testOwner}")
    }

    String createPaymentAccountParameterFor(String testOwner) {
        String parameterName = "payment_account_${testOwner}".toLowerCase()
        String parameterValue = "bank_${testOwner}".toLowerCase()

        // Ensure length constraints
        if (parameterName.length() > 50) {
            parameterName = "payment_${testOwner[0..7]}".toLowerCase()
        }
        if (parameterValue.length() > 50) {
            parameterValue = "bank_${testOwner[0..7]}".toLowerCase()
        }

        jdbcTemplate.update("""
            INSERT INTO func.t_parameter (parameter_name, parameter_value, active_status, date_updated, date_added)
            VALUES (?, ?, true, '1970-01-01 00:00:00.000000', '1970-01-01 00:00:00.000000')
        """, parameterName, parameterValue)

        log.info("Created payment account parameter: ${parameterName}=${parameterValue} for test owner: ${testOwner}")
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
            INSERT INTO func.t_description (description_name, active_status, date_updated, date_added)
            VALUES (?, ?, '1970-01-01 00:00:00.000000', '1970-01-01 00:00:00.000000')
        """, descriptionName, activeStatus)

        log.info("Created description: ${descriptionName} (active: ${activeStatus}) for test owner: ${testOwner}")
        return descriptionName
    }

    void createDescriptionsFor(String testOwner, List<String> descriptionNames) {
        descriptionNames.each { suffix ->
            createDescriptionFor(testOwner, suffix, true)
        }
        log.info("Created ${descriptionNames.size()} descriptions for test owner: ${testOwner}")
    }

    String createTransactionFor(String testOwner, String accountSuffix = "primary",
                                String description = "test_transaction",
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
            INSERT INTO func.t_transaction(account_id, account_type, account_name_owner, guid, transaction_date,
                                      description, category, amount, transaction_state, reoccurring_type,
                                      active_status, notes, receipt_image_id, date_updated, date_added)
            VALUES ((select account_id from func.t_account where account_name_owner=?),
                    (select account_type from func.t_account where account_name_owner=?),
                    ?, ?, '2020-09-04', ?, ?, ?, ?, 'undefined', true, '', null,
                    '2020-10-27 18:51:06.903105', '2020-09-05 20:34:39.360139')
        """, accountName, accountName, accountName, guid, description,
             categoryName, amount, transactionState)

        log.info("Created transaction ${guid} for ${accountName} with amount ${amount}")
        return guid
    }

    String createTransactionWithGuidFor(String testOwner, String guid, String accountSuffix = "primary",
                                       String description = "test_transaction",
                                       BigDecimal amount = new BigDecimal("10.00"),
                                       String category = null,
                                       String transactionState = "cleared") {

        String accountName = "${accountSuffix}_${testOwner}".toLowerCase()
        String categoryName = category ?: "test_category_${testOwner}".toLowerCase()

        // Ensure FK relationships exist
        ensureAccountExists(testOwner, accountSuffix)
        ensureCategoryExists(testOwner, category ?: "test_category")

        jdbcTemplate.update("""
            INSERT INTO func.t_transaction(account_id, account_type, account_name_owner, guid, transaction_date,
                                      description, category, amount, transaction_state, reoccurring_type,
                                      active_status, notes, receipt_image_id, date_updated, date_added, transaction_type)
            VALUES ((select account_id from func.t_account where account_name_owner=?),
                    (select account_type from func.t_account where account_name_owner=?),
                    ?, ?, '2020-09-04', ?, ?, ?, ?, 'undefined', true, '', null,
                    '2020-10-27 18:51:06.903105', '2020-09-05 20:34:39.360139', 'expense')
        """, accountName, accountName, accountName, guid, description,
             categoryName, amount, transactionState)

        log.info("Created transaction ${guid} for ${accountName} with description '${description}' and amount ${amount}")
        return guid
    }

    private void ensureAccountExists(String testOwner, String accountSuffix) {
        String accountName = "${accountSuffix}_${testOwner}".toLowerCase()
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM func.t_account WHERE account_name_owner = ?",
            Integer.class, accountName
        )

        if (count == 0) {
            log.info("Account ${accountName} doesn't exist, creating it")
            createAccountFor(testOwner, accountSuffix, 'credit', true)
        }
    }

    private void ensureCategoryExists(String testOwner, String categorySuffix) {
        String categoryName = "${categorySuffix}_${cleanOwner(testOwner)}".toLowerCase().replaceAll(/[^a-zA-Z0-9]/, '')
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM func.t_category WHERE category_name = ?",
            Integer.class, categoryName
        )

        if (count == 0) {
            log.info("Category ${categoryName} doesn't exist, creating it")
            createCategoryFor(testOwner, categorySuffix, true)
        }
    }

    void createComplexTransactionFor(String testOwner, Map<String, Object> transactionData) {
        String guid = transactionData.guid ?: UUID.randomUUID().toString()
        String accountSuffix = transactionData.accountSuffix ?: "primary"
        String description = transactionData.description ?: "test_transaction_${guid[0..7]}"
        BigDecimal amount = transactionData.amount ?: new BigDecimal("25.00")
        String category = transactionData.category ?: "test_category"
        String transactionState = transactionData.transactionState ?: "cleared"
        String transactionType = transactionData.transactionType ?: "expense"
        String accountType = transactionData.accountType ?: "credit"

        String accountName = accountNameFor(testOwner, accountSuffix)
        String categoryName = "${category}_${cleanOwner(testOwner)}".toLowerCase().replaceAll(/[^a-zA-Z0-9]/, '')

        // Create account if it doesn't exist
        ensureAccountExists(testOwner, accountSuffix)

        // Create category if it doesn't exist
        ensureCategoryExists(testOwner, category)

        jdbcTemplate.update("""
            INSERT INTO func.t_transaction(account_id, account_type, account_name_owner, guid, transaction_date,
                                      description, category, amount, transaction_state, reoccurring_type,
                                      active_status, notes, receipt_image_id, date_updated, date_added, transaction_type)
            VALUES ((select account_id from func.t_account where account_name_owner=?),
                    ?, ?, ?, '2020-09-04', ?, ?, ?, ?, 'undefined', true, '', null,
                    '2020-10-27 18:51:06.903105', '2020-09-05 20:34:39.360139', ?)
        """, accountName, accountType, accountName, guid, description,
             categoryName, amount, transactionState, transactionType)

        log.info("Created complex transaction ${guid} for ${accountName}")
    }

    void createPaymentFor(String testOwner, BigDecimal amount = new BigDecimal("10.00")) {
        String sourceAccount = accountNameFor(testOwner, 'primary')
        String destAccount = accountNameFor(testOwner, 'secondary')

        // Create transactions for both accounts first
        String sourceGuid = UUID.randomUUID().toString()
        String destGuid = UUID.randomUUID().toString()

        // Source transaction (credit account - negative amount)
        jdbcTemplate.update("""
            INSERT INTO func.t_transaction(account_id, account_type, account_name_owner, guid, transaction_date,
                                      description, category, amount, transaction_state, reoccurring_type,
                                      active_status, notes, receipt_image_id, date_updated, date_added)
            VALUES ((select account_id from func.t_account where account_name_owner=?), 'credit', ?, ?, '2020-12-31',
                    'payment_source', ?, ?, 'cleared', 'undefined', true, '', null,
                    '2020-10-27 18:51:06.903105', '2020-09-05 20:34:39.360139')
        """, sourceAccount, sourceAccount, sourceGuid, "test_category_${testOwner}", amount.negate())

        // Destination transaction (debit account - positive amount)
        jdbcTemplate.update("""
            INSERT INTO func.t_transaction(account_id, account_type, account_name_owner, guid, transaction_date,
                                      description, category, amount, transaction_state, reoccurring_type,
                                      active_status, notes, receipt_image_id, date_updated, date_added)
            VALUES ((select account_id from func.t_account where account_name_owner=?), 'debit', ?, ?, '2020-12-31',
                    'payment_dest', ?, ?, 'cleared', 'undefined', true, '', null,
                    '2020-10-27 18:51:06.903105', '2020-09-05 20:34:39.360139')
        """, destAccount, destAccount, destGuid, "test_category_${testOwner}", amount)

        // Create the payment record
        jdbcTemplate.update("""
            INSERT INTO func.t_payment (source_account, destination_account, transaction_date, amount,
                                   guid_source, guid_destination, active_status, date_updated, date_added)
            VALUES (?, ?, '2020-12-31', ?, ?, ?, true, '2021-01-09 14:26:26.739000', '2021-01-09 14:26:26.739000')
        """, sourceAccount, destAccount, amount, sourceGuid, destGuid)

        log.info("Created payment from ${sourceAccount} to ${destAccount} with amount ${amount}")
    }

    void cleanupAccountsFor(String testOwner) {
        log.info("Cleaning up test data for owner: ${testOwner}")

        // Delete in proper order to respect foreign key constraints

        // Delete payments first
        String clean = cleanOwner(testOwner)
        jdbcTemplate.update("DELETE FROM func.t_payment WHERE source_account LIKE ? OR destination_account LIKE ?",
                           "%${testOwner}", "%${testOwner}")
        jdbcTemplate.update("DELETE FROM func.t_payment WHERE source_account LIKE ? OR destination_account LIKE ?",
                           "%${clean}", "%${clean}")

        // Delete transaction categories
        jdbcTemplate.update("DELETE FROM func.t_transaction_categories WHERE transaction_id IN " +
                           "(SELECT transaction_id FROM func.t_transaction WHERE account_name_owner LIKE ?)",
                           "%${testOwner}")
        jdbcTemplate.update("DELETE FROM func.t_transaction_categories WHERE transaction_id IN " +
                           "(SELECT transaction_id FROM func.t_transaction WHERE account_name_owner LIKE ?)",
                           "%${clean}")

        // Delete transactions
        jdbcTemplate.update("DELETE FROM func.t_transaction WHERE account_name_owner LIKE ?", "%${testOwner}")
        jdbcTemplate.update("DELETE FROM func.t_transaction WHERE account_name_owner LIKE ?", "%${clean}")

        // Delete pending transactions (FK constraint requires this before deleting accounts)
        jdbcTemplate.update("DELETE FROM func.t_pending_transaction WHERE account_name_owner LIKE ?", "%${testOwner}")
        jdbcTemplate.update("DELETE FROM func.t_pending_transaction WHERE account_name_owner LIKE ?", "%${clean}")

        // Delete test-specific validation amounts (FK constraint requires this before deleting accounts)
        jdbcTemplate.update("DELETE FROM func.t_validation_amount WHERE account_id IN " +
                           "(SELECT account_id FROM func.t_account WHERE account_name_owner LIKE ?)", "%${testOwner}")
        jdbcTemplate.update("DELETE FROM func.t_validation_amount WHERE account_id IN " +
                           "(SELECT account_id FROM func.t_account WHERE account_name_owner LIKE ?)", "%${clean}")

        // Delete accounts (with error handling for FK constraints)
        try {
            jdbcTemplate.update("DELETE FROM func.t_account WHERE account_name_owner LIKE ?", "%${testOwner}")
        } catch (Exception e) {
            log.warn("Failed to delete accounts for ${testOwner}, possibly due to remaining FK references: ${e.message}")
        }
        try {
            jdbcTemplate.update("DELETE FROM func.t_account WHERE account_name_owner LIKE ?", "%${clean}")
        } catch (Exception e) {
            log.warn("Failed to delete accounts for ${clean}, possibly due to remaining FK references: ${e.message}")
        }

        // Delete test-specific categories
        jdbcTemplate.update("DELETE FROM func.t_category WHERE category_name LIKE ?", "%${testOwner}")
        jdbcTemplate.update("DELETE FROM func.t_category WHERE category_name LIKE ?", "%${clean}")

        // Delete test-specific parameters
        jdbcTemplate.update("DELETE FROM func.t_parameter WHERE parameter_name LIKE ?", "%${testOwner}")
        jdbcTemplate.update("DELETE FROM func.t_parameter WHERE parameter_name LIKE ?", "%${clean}")

        // Delete test-specific descriptions
        jdbcTemplate.update("DELETE FROM func.t_description WHERE description_name LIKE ?", "%${testOwner}")
        jdbcTemplate.update("DELETE FROM func.t_description WHERE description_name LIKE ?", "%${clean}")

        log.info("Successfully cleaned up test data for owner: ${testOwner}")
    }

    Long createValidationAmountFor(String testOwner, BigDecimal amount = new BigDecimal("100.00"), String transactionState = "cleared") {
        // Find the primary account for this test owner
        String accountName = accountNameFor(testOwner, 'primary')
        Long accountId = jdbcTemplate.queryForObject(
            "SELECT account_id FROM func.t_account WHERE account_name_owner = ?",
            Long.class, accountName
        )

        // Insert validation amount
        jdbcTemplate.update("""
            INSERT INTO func.t_validation_amount (account_id, validation_date, active_status, transaction_state, amount, date_updated, date_added)
            VALUES (?, NOW(), true, ?, ?, '1970-01-01 00:00:00.000000', '1970-01-01 00:00:00.000000')
        """, accountId, transactionState, amount)

        // Get the generated validation_id
        Long validationId = jdbcTemplate.queryForObject(
            "SELECT LAST_INSERT_ID()",
            Long.class
        )

        log.info("Created validation amount: ID=${validationId}, accountId=${accountId}, amount=${amount}, state=${transactionState} for test owner: ${testOwner}")
        return validationId
    }

    void createValidationAmountsFor(String testOwner, List<Map<String, Object>> validationData) {
        validationData.each { data ->
            BigDecimal amount = data.amount as BigDecimal ?: new BigDecimal("100.00")
            String state = data.transactionState as String ?: "cleared"
            createValidationAmountFor(testOwner, amount, state)
        }
        log.info("Created ${validationData.size()} validation amounts for test owner: ${testOwner}")
    }

    void cleanupAllTestData() {
        log.info("Performing full test data cleanup")

        // Clean up any test data that might have been left behind
        jdbcTemplate.update("DELETE FROM func.t_payment WHERE source_account LIKE 'test_%' OR destination_account LIKE 'test_%'")
        jdbcTemplate.update("DELETE FROM func.t_payment WHERE source_account LIKE 'primary_%' OR destination_account LIKE 'primary_%'")
        jdbcTemplate.update("DELETE FROM func.t_payment WHERE source_account LIKE 'secondary_%' OR destination_account LIKE 'secondary_%'")

        jdbcTemplate.update("DELETE FROM func.t_transaction_categories WHERE transaction_id IN " +
                           "(SELECT transaction_id FROM func.t_transaction WHERE account_name_owner LIKE 'test_%')")
        jdbcTemplate.update("DELETE FROM func.t_transaction_categories WHERE transaction_id IN " +
                           "(SELECT transaction_id FROM func.t_transaction WHERE account_name_owner LIKE 'primary_%')")
        jdbcTemplate.update("DELETE FROM func.t_transaction_categories WHERE transaction_id IN " +
                           "(SELECT transaction_id FROM func.t_transaction WHERE account_name_owner LIKE 'secondary_%')")

        jdbcTemplate.update("DELETE FROM func.t_transaction WHERE account_name_owner LIKE 'test_%'")
        jdbcTemplate.update("DELETE FROM func.t_transaction WHERE account_name_owner LIKE 'primary_%'")
        jdbcTemplate.update("DELETE FROM func.t_transaction WHERE account_name_owner LIKE 'secondary_%'")

        jdbcTemplate.update("DELETE FROM func.t_account WHERE account_name_owner LIKE 'test_%'")
        jdbcTemplate.update("DELETE FROM func.t_account WHERE account_name_owner LIKE 'primary_%'")
        jdbcTemplate.update("DELETE FROM func.t_account WHERE account_name_owner LIKE 'secondary_%'")

        jdbcTemplate.update("DELETE FROM func.t_category WHERE category_name LIKE 'test_%'")
        jdbcTemplate.update("DELETE FROM func.t_parameter WHERE parameter_name LIKE 'test_%'")

        // Clean up validation amounts
        jdbcTemplate.update("DELETE FROM func.t_validation_amount WHERE account_id IN " +
                           "(SELECT account_id FROM func.t_account WHERE account_name_owner LIKE 'test_%')")

        log.info("Completed full test data cleanup")
    }
}
