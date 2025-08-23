package finance.helpers

import finance.domain.Account
import finance.domain.AccountType
import finance.domain.Category
import finance.domain.Description
import finance.domain.Parameter
import finance.domain.Payment
import finance.domain.Transaction
import finance.domain.ValidationAmount
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Slf4j
@Component
class TestFixtures {

    @Autowired
    TestDataManager testDataManager

    /**
     * Creates a complete test context for account-related testing
     * Includes primary/secondary accounts with all required relationships
     */
    AccountTestContext createAccountTestContext(String testOwner) {
        log.info("Creating account test context for owner: ${testOwner}")

        // Create base accounts through TestDataManager
        testDataManager.createMinimalAccountsFor(testOwner)

        // Generate pattern-compliant account names matching TestDataManager logic
        String cleanOwner = testOwner.replaceAll(/[^a-z]/, '').toLowerCase()
        if (cleanOwner.isEmpty()) cleanOwner = "testowner"

        return new AccountTestContext(
            testOwner: testOwner,
            primaryAccountName: "primary_${cleanOwner}".toLowerCase(),
            secondaryAccountName: "secondary_${cleanOwner}".toLowerCase(),
            categoryName: "test_category_${testOwner}".toLowerCase(),
            testDataManager: testDataManager
        )
    }

    /**
     * Creates test context with transaction data for payment/transfer testing
     */
    TransactionTestContext createTransactionContext(String testOwner, BigDecimal amount = new BigDecimal("25.00")) {
        AccountTestContext accountContext = createAccountTestContext(testOwner)

        // Don't create sample transactions in setup - let individual tests create them as needed
        return new TransactionTestContext(
            testOwner: testOwner,
            accountContext: accountContext,
            sampleAmount: amount,
            defaultTransactionGuid: null,
            secondaryTransactionGuid: null,
            testDataManager: testDataManager
        )
    }

    /**
     * Creates test context for complex payment scenarios
     */
    PaymentTestContext createPaymentContext(String testOwner, BigDecimal amount = new BigDecimal("50.00")) {
        TransactionTestContext transactionContext = createTransactionContext(testOwner, amount)

        // Don't create actual payment data in setup - let individual tests create them as needed
        return new PaymentTestContext(
            transactionContext: transactionContext,
            paymentAmount: amount,
            testDataManager: testDataManager
        )
    }

    /**
     * Creates test context for category-related operations
     */
    CategoryTestContext createCategoryTestContext(String testOwner) {
        log.info("Creating category test context for owner: ${testOwner}")

        // Create base categories for testing
        List<String> defaultCategories = ['online', 'groceries', 'utilities']
        testDataManager.createCategoriesFor(testOwner, defaultCategories)

        return new CategoryTestContext(
            testOwner: testOwner,
            defaultCategories: defaultCategories.collect {
                String ownerPart = testOwner.replaceAll(/[^a-z0-9]/, '')
                if (ownerPart.isEmpty()) ownerPart = "test"
                "${it}_${ownerPart}".toLowerCase()
            },
            testDataManager: testDataManager
        )
    }

    /**
     * Creates test context for parameter-related operations
     */
    ParameterTestContext createParameterTestContext(String testOwner) {
        log.info("Creating parameter test context for owner: ${testOwner}")

        // Create default parameters for testing
        Map<String, String> defaultParameters = [
            'payment_account': 'bank_account',
            'config_setting': 'config_value',
            'app_setting': 'app_value'
        ]
        testDataManager.createParametersFor(testOwner, defaultParameters)

        return new ParameterTestContext(
            testOwner: testOwner,
            defaultParameters: defaultParameters.collectEntries { nameSuffix, valueSuffix ->
                [("${nameSuffix}_${testOwner}".toLowerCase()): ("${valueSuffix}_${testOwner}".toLowerCase())]
            },
            testDataManager: testDataManager
        )
    }

    /**
     * Creates test context for description-related operations
     */
    DescriptionTestContext createDescriptionTestContext(String testOwner) {
        log.info("Creating description test context for owner: ${testOwner}")

        // Create default descriptions for testing
        List<String> defaultDescriptions = ['store', 'restaurant', 'service', 'online']
        testDataManager.createDescriptionsFor(testOwner, defaultDescriptions)

        return new DescriptionTestContext(
            testOwner: testOwner,
            defaultDescriptions: defaultDescriptions.collect { "${it}_${testOwner}".toLowerCase() },
            testDataManager: testDataManager
        )
    }

    /**
     * Creates test context for validation amount-related operations
     */
    ValidationAmountTestContext createValidationAmountTestContext(String testOwner) {
        log.info("Creating validation amount test context for owner: ${testOwner}")

        // Create base accounts for validation amounts (needed for accountId relationship)
        testDataManager.createMinimalAccountsFor(testOwner)

        return new ValidationAmountTestContext(
            testOwner: testOwner,
            testDataManager: testDataManager
        )
    }
}

/**
 * Test context for account-related operations
 * Provides easy access to test data and helper methods
 */
class AccountTestContext {
    String testOwner
    String primaryAccountName
    String secondaryAccountName
    String categoryName
    TestDataManager testDataManager

    Account createUniqueAccount(String prefix = "unique") {
        return SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName(prefix)
                .buildAndValidate()
    }

    Account createCreditAccount(String accountName) {
        return SmartAccountBuilder.builderForOwner(testOwner)
                .withAccountNameOwner(accountName)
                .asCredit()
                .buildAndValidate()
    }

    Account createDebitAccount(String accountName) {
        return SmartAccountBuilder.builderForOwner(testOwner)
                .withAccountNameOwner(accountName)
                .asDebit()
                .buildAndValidate()
    }

    Account createInactiveAccount(String accountName) {
        return SmartAccountBuilder.builderForOwner(testOwner)
                .withAccountNameOwner(accountName)
                .asInactive()
                .buildAndValidate()
    }

    String createAdditionalAccount(String suffix, String accountType = 'credit') {
        return testDataManager.createAccountFor(testOwner, suffix, accountType)
    }

    void cleanup() {
        testDataManager.cleanupAccountsFor(testOwner)
    }
}

/**
 * Test context for transaction-related operations
 */
class TransactionTestContext {
    String testOwner
    AccountTestContext accountContext
    BigDecimal sampleAmount
    String defaultTransactionGuid
    String secondaryTransactionGuid
    TestDataManager testDataManager

    Transaction createUniqueTransaction(String prefix = "unique") {
        return SmartTransactionBuilder.builderForOwner(testOwner)
                .withUniqueGuid()
                .withUniqueDescription(prefix)
                .buildAndValidate()
    }

    Transaction createActiveTransaction(String description) {
        return SmartTransactionBuilder.builderForOwner(testOwner)
                .withDescription(description)
                .asActive()
                .buildAndValidate()
    }

    Transaction createInactiveTransaction(String description) {
        return SmartTransactionBuilder.builderForOwner(testOwner)
                .withDescription(description)
                .asInactive()
                .buildAndValidate()
    }

    Transaction createExpenseTransaction(String description, BigDecimal amount) {
        return SmartTransactionBuilder.builderForOwner(testOwner)
                .withDescription(description)
                .withAmount(amount)
                .asExpense()
                .buildAndValidate()
    }

    Transaction createIncomeTransaction(String description, BigDecimal amount) {
        return SmartTransactionBuilder.builderForOwner(testOwner)
                .withDescription(description)
                .withAmount(amount)
                .asIncome()
                .buildAndValidate()
    }

    Transaction createCreditTransaction(String accountName, String description) {
        return SmartTransactionBuilder.builderForOwner(testOwner)
                .withAccountNameOwner(accountName)
                .withDescription(description)
                .asCredit()
                .buildAndValidate()
    }

    Transaction createDebitTransaction(String accountName, String description) {
        return SmartTransactionBuilder.builderForOwner(testOwner)
                .withAccountNameOwner(accountName)
                .withDescription(description)
                .asDebit()
                .buildAndValidate()
    }

    Transaction createOnlineTransaction() {
        return SmartTransactionBuilder.builderForOwner(testOwner)
                .asOnlineTransaction()
                .buildAndValidate()
    }

    Transaction createGroceryTransaction() {
        return SmartTransactionBuilder.builderForOwner(testOwner)
                .asGroceryTransaction()
                .buildAndValidate()
    }

    Transaction createUtilityTransaction() {
        return SmartTransactionBuilder.builderForOwner(testOwner)
                .asUtilityTransaction()
                .buildAndValidate()
    }

    Transaction createRestaurantTransaction() {
        return SmartTransactionBuilder.builderForOwner(testOwner)
                .asRestaurantTransaction()
                .buildAndValidate()
    }

    Transaction createClearedTransaction(String description) {
        return SmartTransactionBuilder.builderForOwner(testOwner)
                .withDescription(description)
                .asCleared()
                .buildAndValidate()
    }

    Transaction createPendingTransaction(String description) {
        return SmartTransactionBuilder.builderForOwner(testOwner)
                .withDescription(description)
                .asPending()
                .buildAndValidate()
    }

    Transaction createOutstandingTransaction(String description) {
        return SmartTransactionBuilder.builderForOwner(testOwner)
                .withDescription(description)
                .asOutstanding()
                .buildAndValidate()
    }

    Transaction createFutureTransaction(String description) {
        return SmartTransactionBuilder.builderForOwner(testOwner)
                .withDescription(description)
                .asFuture()
                .buildAndValidate()
    }

    String createTransactionInDatabase(String accountSuffix, String description, BigDecimal amount) {
        return testDataManager.createTransactionFor(testOwner, accountSuffix, description, amount)
    }

    String createComplexTransactionInDatabase(Map<String, Object> transactionData) {
        testDataManager.createComplexTransactionFor(testOwner, transactionData)
        return transactionData.guid ?: UUID.randomUUID().toString()
    }

    String getFirstDefaultTransactionGuid() {
        return defaultTransactionGuid
    }

    String getSecondaryTransactionGuid() {
        return secondaryTransactionGuid
    }

    void cleanup() {
        accountContext.cleanup()
    }
}

/**
 * Test context for payment-related operations
 */
class PaymentTestContext {
    TransactionTestContext transactionContext
    BigDecimal paymentAmount
    TestDataManager testDataManager

    Payment createUniquePayment(String prefix = "unique", BigDecimal amount = null) {
        BigDecimal actualAmount = amount ?: paymentAmount
        Payment payment = SmartPaymentBuilder.builderForOwner(transactionContext.testOwner)
                .withUniqueAccounts(prefix, "dest${prefix}")
                .withAmount(actualAmount)
                .buildAndValidate()

        // Create the accounts that this payment references
        createAccountsForPayment(payment)

        return payment
    }

    private void createAccountsForPayment(Payment payment) {
        // The payment account names are in format: "prefix_owner"
        // We need to create accounts using the raw account names directly
        createAccountDirectly(payment.sourceAccount, 'debit')
        createAccountDirectly(payment.destinationAccount, 'credit')
    }

    private void createAccountDirectly(String accountName, String accountType) {
        // Create account directly with the full account name
        testDataManager.jdbcTemplate.update("""
            INSERT INTO func.t_account(account_name_owner, account_type, active_status, moniker,
                                  date_closed, date_updated, date_added)
            VALUES (?, ?, true, '0000', '1969-12-31 18:00:00.000000',
                    '2020-12-23 20:04:37.903600', '2020-09-05 20:33:34.077330')
        """, accountName, accountType)
    }

    Payment createActivePayment(String prefix, BigDecimal amount = null) {
        BigDecimal actualAmount = amount ?: paymentAmount
        Payment payment = SmartPaymentBuilder.builderForOwner(transactionContext.testOwner)
                .withUniqueAccounts(prefix, "dest${prefix}")
                .withAmount(actualAmount)
                .asActive()
                .buildAndValidate()

        // Create the accounts that this payment references
        createAccountsForPayment(payment)

        return payment
    }

    Payment createInactivePayment(String prefix, BigDecimal amount = null) {
        BigDecimal actualAmount = amount ?: paymentAmount
        Payment payment = SmartPaymentBuilder.builderForOwner(transactionContext.testOwner)
                .withUniqueAccounts(prefix, "dest${prefix}")
                .withAmount(actualAmount)
                .asInactive()
                .buildAndValidate()

        // Create the accounts that this payment references
        createAccountsForPayment(payment)

        return payment
    }

    Payment createSmallPayment(String prefix) {
        String counter = System.currentTimeMillis().toString().takeLast(6)
        return SmartPaymentBuilder.builderForOwner(transactionContext.accountContext.testOwner)
                .withSourceAccount("${prefix}${counter}_${transactionContext.accountContext.testOwner}".toLowerCase())
                .withDestinationAccount("dest${counter}_${transactionContext.accountContext.testOwner}".toLowerCase())
                .withAmount(new BigDecimal("5.00"))
                .build()
    }

    Payment createLargePayment(String prefix) {
        String counter = System.currentTimeMillis().toString().takeLast(6)
        return SmartPaymentBuilder.builderForOwner(transactionContext.accountContext.testOwner)
                .withSourceAccount("${prefix}${counter}_${transactionContext.accountContext.testOwner}".toLowerCase())
                .withDestinationAccount("dest${counter}_${transactionContext.accountContext.testOwner}".toLowerCase())
                .withAmount(new BigDecimal("500.00"))
                .build()
    }

    void createPayment(BigDecimal amount) {
        testDataManager.createPaymentFor(transactionContext.accountContext.testOwner, amount)
    }

    void cleanup() {
        transactionContext.cleanup()
    }
}

/**
 * Test context for category-related operations
 */
class CategoryTestContext {
    String testOwner
    List<String> defaultCategories
    TestDataManager testDataManager

    Category createUniqueCategory(String prefix = "unique") {
        return SmartCategoryBuilder.builderForOwner(testOwner)
                .withUniqueCategoryName(prefix)
                .buildAndValidate()
    }

    Category createActiveCategory(String categoryName) {
        return SmartCategoryBuilder.builderForOwner(testOwner)
                .withCategoryName(categoryName)
                .asActive()
                .buildAndValidate()
    }

    Category createInactiveCategory(String categoryName) {
        return SmartCategoryBuilder.builderForOwner(testOwner)
                .withCategoryName(categoryName)
                .asInactive()
                .buildAndValidate()
    }

    Category createOnlineCategory() {
        return SmartCategoryBuilder.builderForOwner(testOwner)
                .asOnlineCategory()
                .buildAndValidate()
    }

    String createAdditionalCategory(String suffix, boolean activeStatus = true) {
        return testDataManager.createCategoryFor(testOwner, suffix, activeStatus)
    }

    List<String> getDefaultCategoryNames() {
        return defaultCategories.collect()
    }

    void cleanup() {
        testDataManager.cleanupAccountsFor(testOwner)
    }
}

/**
 * Test context for parameter-related operations
 */
class ParameterTestContext {
    String testOwner
    Map<String, String> defaultParameters  // parameterName -> parameterValue
    TestDataManager testDataManager

    Parameter createUniqueParameter(String namePrefix = "unique", String valuePrefix = "value") {
        return SmartParameterBuilder.builderForOwner(testOwner)
                .withUniqueParameterName(namePrefix)
                .withUniqueParameterValue(valuePrefix)
                .buildAndValidate()
    }

    Parameter createActiveParameter(String parameterName, String parameterValue) {
        return SmartParameterBuilder.builderForOwner(testOwner)
                .withParameterName(parameterName)
                .withParameterValue(parameterValue)
                .asActive()
                .buildAndValidate()
    }

    Parameter createInactiveParameter(String parameterName, String parameterValue) {
        return SmartParameterBuilder.builderForOwner(testOwner)
                .withParameterName(parameterName)
                .withParameterValue(parameterValue)
                .asInactive()
                .buildAndValidate()
    }

    Parameter createPaymentAccountParameter() {
        return SmartParameterBuilder.builderForOwner(testOwner)
                .asPaymentAccountParameter()
                .buildAndValidate()
    }

    Parameter createConfigParameter(String configType) {
        return SmartParameterBuilder.builderForOwner(testOwner)
                .asConfigParameter(configType)
                .buildAndValidate()
    }

    String createAdditionalParameter(String nameSuffix, String valueSuffix, boolean activeStatus = true) {
        return testDataManager.createParameterFor(testOwner, nameSuffix, valueSuffix, activeStatus)
    }

    Map<String, String> getDefaultParameterPairs() {
        return defaultParameters.collectEntries()
    }

    String getFirstDefaultParameterName() {
        return defaultParameters.keySet().first()
    }

    String getFirstDefaultParameterValue() {
        return defaultParameters.values().first()
    }

    void cleanup() {
        testDataManager.cleanupAccountsFor(testOwner)
    }
}

/**
 * Test context for description-related operations
 */
class DescriptionTestContext {
    String testOwner
    List<String> defaultDescriptions
    TestDataManager testDataManager

    Description createUniqueDescription(String prefix = "unique") {
        return SmartDescriptionBuilder.builderForOwner(testOwner)
                .withUniqueDescriptionName(prefix)
                .buildAndValidate()
    }

    Description createActiveDescription(String descriptionName) {
        return SmartDescriptionBuilder.builderForOwner(testOwner)
                .withDescriptionName(descriptionName)
                .asActive()
                .buildAndValidate()
    }

    Description createInactiveDescription(String descriptionName) {
        return SmartDescriptionBuilder.builderForOwner(testOwner)
                .withDescriptionName(descriptionName)
                .asInactive()
                .buildAndValidate()
    }

    Description createStoreDescription() {
        return SmartDescriptionBuilder.builderForOwner(testOwner)
                .asStoreDescription()
                .buildAndValidate()
    }

    Description createRestaurantDescription() {
        return SmartDescriptionBuilder.builderForOwner(testOwner)
                .asRestaurantDescription()
                .buildAndValidate()
    }

    Description createServiceDescription() {
        return SmartDescriptionBuilder.builderForOwner(testOwner)
                .asServiceDescription()
                .buildAndValidate()
    }

    Description createOnlineDescription() {
        return SmartDescriptionBuilder.builderForOwner(testOwner)
                .asOnlineDescription()
                .buildAndValidate()
    }

    String createAdditionalDescription(String suffix, boolean activeStatus = true) {
        return testDataManager.createDescriptionFor(testOwner, suffix, activeStatus)
    }

    List<String> getDefaultDescriptionNames() {
        return defaultDescriptions.collect()
    }

    String getFirstDefaultDescription() {
        return defaultDescriptions[0]
    }

    void cleanup() {
        testDataManager.cleanupAccountsFor(testOwner)
    }
}

/**
 * Test context for validation amount-related operations
 */
class ValidationAmountTestContext {
    String testOwner
    TestDataManager testDataManager

    // Get the accountId for the test owner's primary account
    private Long getPrimaryAccountId() {
        return testDataManager.jdbcTemplate.queryForObject(
            "SELECT account_id FROM func.t_account WHERE account_name_owner = ?",
            Long.class, "primary_${testOwner}".toLowerCase()
        )
    }

    ValidationAmount createUniqueValidationAmount(String prefix = "unique") {
        return SmartValidationAmountBuilder.builderForOwner(testOwner)
                .withAccountId(getPrimaryAccountId())
                .withAmount(new BigDecimal("${100 + Math.abs(prefix.hashCode() % 900)}.00"))
                .build()  // Use build() instead of buildAndValidate() for simplicity
    }

    ValidationAmount createClearedValidationAmount(BigDecimal amount = new BigDecimal("100.00")) {
        return SmartValidationAmountBuilder.builderForOwner(testOwner)
                .withAccountId(getPrimaryAccountId())
                .withAmount(amount)
                .asCleared()
                .build()
    }

    ValidationAmount createOutstandingValidationAmount(BigDecimal amount = new BigDecimal("200.00")) {
        return SmartValidationAmountBuilder.builderForOwner(testOwner)
                .withAccountId(getPrimaryAccountId())
                .withAmount(amount)
                .asOutstanding()
                .build()
    }

    ValidationAmount createFutureValidationAmount(BigDecimal amount = new BigDecimal("300.00")) {
        return SmartValidationAmountBuilder.builderForOwner(testOwner)
                .withAccountId(getPrimaryAccountId())
                .withAmount(amount)
                .asFuture()
                .build()
    }

    ValidationAmount createActiveValidationAmount(BigDecimal amount = new BigDecimal("150.00")) {
        return SmartValidationAmountBuilder.builderForOwner(testOwner)
                .withAccountId(getPrimaryAccountId())
                .withAmount(amount)
                .asActive()
                .build()
    }

    ValidationAmount createInactiveValidationAmount(BigDecimal amount = new BigDecimal("250.00")) {
        return SmartValidationAmountBuilder.builderForOwner(testOwner)
                .withAccountId(getPrimaryAccountId())
                .withAmount(amount)
                .asInactive()
                .build()
    }

    ValidationAmount createSmallValidationAmount() {
        return SmartValidationAmountBuilder.builderForOwner(testOwner)
                .withAccountId(getPrimaryAccountId())
                .withAmount(new BigDecimal("5.00"))
                .build()
    }

    ValidationAmount createLargeValidationAmount() {
        return SmartValidationAmountBuilder.builderForOwner(testOwner)
                .withAccountId(getPrimaryAccountId())
                .withAmount(new BigDecimal("99999999.99"))
                .build()
    }

    Long createPersistentValidationAmount(BigDecimal amount, String transactionState = "cleared") {
        return testDataManager.createValidationAmountFor(testOwner, amount, transactionState)
    }

    void cleanup() {
        testDataManager.cleanupAccountsFor(testOwner)
    }
}