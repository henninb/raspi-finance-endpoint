package finance.helpers

import finance.domain.Account
import finance.domain.AccountType
import finance.domain.Category
import finance.domain.Description
import finance.domain.ImageFormatType
import finance.domain.Parameter
import finance.domain.Payment
import finance.domain.ReceiptImage
import finance.domain.Transaction
import finance.domain.Transfer
import finance.domain.User
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
     * Creates a complete test context for integration repository testing
     * Includes primary/secondary accounts with all required relationships
     */
    RepositoryTestContext createRepositoryTestContext(String testOwner) {
        log.info("Creating repository test context for integration test owner: ${testOwner}")

        // Initialize integration test environment
        testDataManager.initializeIntegrationTestEnvironment(testOwner)

        // Generate pattern-compliant account names matching TestDataManager logic
        String cleanOwner = testOwner.replaceAll(/[^a-z]/, '').toLowerCase()
        if (cleanOwner.isEmpty()) cleanOwner = "testowner"

        return new RepositoryTestContext(
            testOwner: testOwner,
            primaryAccountName: "primary_${cleanOwner}".toLowerCase(),
            secondaryAccountName: "secondary_${cleanOwner}".toLowerCase(),
            categoryName: "test_category_${cleanOwner}".toLowerCase(),
            testDataManager: testDataManager
        )
    }

    /**
     * Creates test context for service integration testing
     */
    ServiceIntegrationContext createServiceIntegrationContext(String testOwner) {
        log.info("Creating service integration context for test owner: ${testOwner}")

        // Initialize integration test environment
        testDataManager.initializeIntegrationTestEnvironment(testOwner)

        return new ServiceIntegrationContext(
            testOwner: testOwner,
            testDataManager: testDataManager
        )
    }

    /**
     * Creates test context for GraphQL resolver integration testing
     */
    GraphQLIntegrationContext createGraphQLIntegrationContext(String testOwner) {
        log.info("Creating GraphQL integration context for test owner: ${testOwner}")

        // Initialize integration test environment
        testDataManager.initializeIntegrationTestEnvironment(testOwner)

        return new GraphQLIntegrationContext(
            testOwner: testOwner,
            testDataManager: testDataManager
        )
    }

    /**
     * Creates test context for security integration testing
     */
    SecurityIntegrationContext createSecurityIntegrationContext(String testOwner) {
        log.info("Creating security integration context for test owner: ${testOwner}")

        return new SecurityIntegrationContext(
            testOwner: testOwner,
            testDataManager: testDataManager
        )
    }


}

/**
 * Test context for repository integration testing
 * Provides easy access to test data and helper methods for database operations
 */
class RepositoryTestContext {
    String testOwner
    String primaryAccountName
    String secondaryAccountName
    String categoryName
    TestDataManager testDataManager

    Long createTestAccount(String prefix = "test", AccountType type = AccountType.Debit) {
        return testDataManager.createAccountForIntegrationTest(testOwner, prefix, type.toString().toLowerCase())
    }

    Transaction createTestTransaction(Long accountId, String description = "integration_test_transaction") {
        return SmartTransactionBuilder.builderForOwner(testOwner)
                .withAccountId(accountId)
                .withDescription(description)
                .buildAndValidate()
    }

    Account createUniqueAccount(String prefix = "unique") {
        return SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName(prefix)
                .buildAndValidate()
    }

    Category createUniqueCategory(String prefix = "unique") {
        return SmartCategoryBuilder.builderForOwner(testOwner)
                .withUniqueCategoryName(prefix)
                .buildAndValidate()
    }

    Description createUniqueDescription(String prefix = "unique") {
        return SmartDescriptionBuilder.builderForOwner(testOwner)
                .withUniqueDescriptionName(prefix)
                .buildAndValidate()
    }

    Transaction createUniqueTransaction(String prefix = "unique") {
        return SmartTransactionBuilder.builderForOwner(testOwner)
                .withUniqueGuid()
                .withUniqueDescription(prefix)
                .buildAndValidate()
    }

    ValidationAmount createTestValidationAmount(Long accountId, BigDecimal amount = new BigDecimal("100.00")) {
        return SmartValidationAmountBuilder.builderForOwner(testOwner)
                .withAccountId(accountId)
                .withAmount(amount)
                .buildAndValidate()
    }

    User createTestUser(String prefix = "user", String password = "testpassword123") {
        return SmartUserBuilder.builderForOwner(testOwner)
                .withUniqueUsername(prefix)
                .withFirstName("Test")
                .withLastName("User")
                .withPassword(password)
                .asActive()
                .buildAndValidate()
    }

    void cleanup() {
        testDataManager.cleanupIntegrationTestsFor(testOwner)
    }
}

/**
 * Test context for service layer integration testing
 */
class ServiceIntegrationContext {
    String testOwner
    TestDataManager testDataManager

    AccountServiceTestScenario createAccountServiceScenario() {
        return new AccountServiceTestScenario(testOwner, testDataManager)
    }

    TransactionServiceTestScenario createTransactionServiceScenario() {
        return new TransactionServiceTestScenario(testOwner, testDataManager)
    }

    PaymentServiceTestScenario createPaymentServiceScenario() {
        return new PaymentServiceTestScenario(testOwner, testDataManager)
    }

    void cleanup() {
        testDataManager.cleanupIntegrationTestsFor(testOwner)
    }
}

/**
 * Test scenario for account service integration testing
 */
class AccountServiceTestScenario {
    String testOwner
    TestDataManager testDataManager

    AccountServiceTestScenario(String testOwner, TestDataManager testDataManager) {
        this.testOwner = testOwner
        this.testDataManager = testDataManager
    }

    Long createAccountWithTransactions(List<Map<String, Object>> transactionData) {
        // Create account first
        Long accountId = testDataManager.createAccountForIntegrationTest(testOwner, "service", "debit")

        // Create transactions for the account
        transactionData.each { txnData ->
            String description = txnData.description ?: "service_test_transaction"
            BigDecimal amount = txnData.amount ?: new BigDecimal("100.00")
            String state = txnData.state ?: "cleared"

            testDataManager.createTransactionFor(testOwner, "service", description, amount, state)
        }

        return accountId
    }

    Account createTestAccount(String accountType = "debit") {
        return SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("service")
                .withAccountType(AccountType.valueOf(accountType.capitalize()))
                .buildAndValidate()
    }
}

/**
 * Test scenario for transaction service integration testing
 */
class TransactionServiceTestScenario {
    String testOwner
    TestDataManager testDataManager

    TransactionServiceTestScenario(String testOwner, TestDataManager testDataManager) {
        this.testOwner = testOwner
        this.testDataManager = testDataManager
    }

    Transaction createTestTransaction(String description = "service_integration_test") {
        return SmartTransactionBuilder.builderForOwner(testOwner)
                .withDescription(description)
                .buildAndValidate()
    }
}

/**
 * Test scenario for payment service integration testing
 */
class PaymentServiceTestScenario {
    String testOwner
    TestDataManager testDataManager
    Long sourceAccountId
    Long destinationAccountId
    String sourceAccountName
    String destinationAccountName

    PaymentServiceTestScenario(String testOwner, TestDataManager testDataManager) {
        this.testOwner = testOwner
        this.testDataManager = testDataManager

        // Create source and destination accounts for payment scenarios
        this.sourceAccountId = testDataManager.createAccountForIntegrationTest(testOwner, "source", "debit")
        this.destinationAccountId = testDataManager.createAccountForIntegrationTest(testOwner, "dest", "credit")

        // Get the actual account names created
        this.sourceAccountName = "source_${testOwner.replaceAll(/[^a-z]/, '').toLowerCase()}"
        this.destinationAccountName = "dest_${testOwner.replaceAll(/[^a-z]/, '').toLowerCase()}"
    }

    Payment createTestPayment(BigDecimal amount = new BigDecimal("100.00")) {
        return SmartPaymentBuilder.builderForOwner(testOwner)
                .withSourceAccount(sourceAccountName)
                .withDestinationAccount(destinationAccountName)
                .withAmount(amount)
                .buildAndValidate()
    }
}

/**
 * Test context for GraphQL resolver integration testing
 */
class GraphQLIntegrationContext {
    String testOwner
    TestDataManager testDataManager

    PaymentTestScenario createPaymentScenario() {
        Long sourceAccountId = testDataManager.createAccountForIntegrationTest(testOwner, "source", "debit")
        Long destAccountId = testDataManager.createAccountForIntegrationTest(testOwner, "dest", "credit")
        return new PaymentTestScenario(sourceAccountId, destAccountId, testDataManager, testOwner)
    }

    TransferTestScenario createTransferScenario() {
        Long sourceAccountId = testDataManager.createAccountForIntegrationTest(testOwner, "source", "debit")
        Long destAccountId = testDataManager.createAccountForIntegrationTest(testOwner, "dest", "debit")
        return new TransferTestScenario(sourceAccountId, destAccountId, testDataManager, testOwner)
    }

    void cleanup() {
        testDataManager.cleanupIntegrationTestsFor(testOwner)
    }
}

/**
 * Test scenario for payment GraphQL testing
 */
class PaymentTestScenario {
    Long sourceAccountId
    Long destinationAccountId
    String sourceAccountName
    String destinationAccountName
    TestDataManager testDataManager
    String testOwner

    PaymentTestScenario(Long sourceAccountId, Long destAccountId, TestDataManager testDataManager, String testOwner) {
        this.sourceAccountId = sourceAccountId
        this.destinationAccountId = destAccountId
        this.testDataManager = testDataManager
        this.testOwner = testOwner

        // Generate the account names that were created
        String cleanOwner = testOwner.replaceAll(/[^a-z]/, '').toLowerCase()
        this.sourceAccountName = "source_${cleanOwner}"
        this.destinationAccountName = "dest_${cleanOwner}"
    }

    Payment createPaymentForGraphQL(BigDecimal amount = new BigDecimal("100.00")) {
        return SmartPaymentBuilder.builderForOwner(testOwner)
                .withSourceAccount(sourceAccountName)
                .withDestinationAccount(destinationAccountName)
                .withAmount(amount)
                .buildAndValidate()
    }
}

/**
 * Test scenario for transfer GraphQL testing
 */
class TransferTestScenario {
    Long sourceAccountId
    Long destinationAccountId
    String sourceAccountName
    String destinationAccountName
    TestDataManager testDataManager
    String testOwner

    TransferTestScenario(Long sourceAccountId, Long destAccountId, TestDataManager testDataManager, String testOwner) {
        this.sourceAccountId = sourceAccountId
        this.destinationAccountId = destAccountId
        this.testDataManager = testDataManager
        this.testOwner = testOwner

        // Generate the account names that were created
        String cleanOwner = testOwner.replaceAll(/[^a-z]/, '').toLowerCase()
        this.sourceAccountName = "source_${cleanOwner}"
        this.destinationAccountName = "dest_${cleanOwner}"
    }

    Transfer createTransferForGraphQL(BigDecimal amount = new BigDecimal("100.00")) {
        return SmartTransferBuilder.builderForOwner(testOwner)
                .withSourceAccount(sourceAccountName)
                .withDestinationAccount(destinationAccountName)
                .withAmount(amount)
                .buildAndValidate()
    }
}

/**
 * Test context for security integration testing
 */
class SecurityIntegrationContext {
    String testOwner
    TestDataManager testDataManager

    User createTestUser(String username = null, String password = null) {
        String actualUsername = username ?: "test_user_${testOwner}"
        String actualPassword = password ?: "test_password_${System.currentTimeMillis()}"

        return SmartUserBuilder.builderForOwner(testOwner)
                .withUsername(actualUsername)
                .withPassword(actualPassword)
                .buildAndValidate()
    }

    void cleanup() {
        testDataManager.cleanupIntegrationTestsFor(testOwner)
    }
}

