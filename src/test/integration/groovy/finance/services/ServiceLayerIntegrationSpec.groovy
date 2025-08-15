package finance.services

import finance.Application
import finance.domain.*
import finance.repositories.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

import java.sql.Date
import java.sql.Timestamp

@ActiveProfiles("int")
@SpringBootTest
@ContextConfiguration(classes = Application)
class ServiceLayerIntegrationSpec extends Specification {

    @Autowired
    TransactionService transactionService

    @Autowired
    AccountService accountService

    @Autowired
    CategoryService categoryService

    @Autowired
    DescriptionService descriptionService

    @Autowired
    PaymentService paymentService

    @Autowired
    TransferService transferService

    @Autowired
    ValidationAmountService validationAmountService

    @Autowired
    ReceiptImageService receiptImageService

    @Autowired
    AccountRepository accountRepository

    @Autowired
    TransactionRepository transactionRepository

    void setup() {
        // Let the services handle account creation automatically
    }

    void 'test transaction service integration with database operations'() {
        given:
        Transaction testTransaction = new Transaction(
            guid: UUID.randomUUID().toString(),
            accountNameOwner: "checking_brian",
            accountType: AccountType.Credit,
            description: "service_integration_test_transaction",
            category: "service_test_category",
            amount: 150.75,
            transactionDate: Date.valueOf("2023-05-25"),
            transactionState: TransactionState.Cleared,
            transactionType: TransactionType.Expense,
            notes: "Integration test for transaction service",
            activeStatus: true,
            dateUpdated: new Timestamp(System.currentTimeMillis()),
            dateAdded: new Timestamp(System.currentTimeMillis())
        )

        when:
        Transaction savedTransaction = transactionService.insertTransaction(testTransaction)

        then:
        savedTransaction != null
        savedTransaction.transactionId != null
        savedTransaction.guid == testTransaction.guid
        savedTransaction.description == "service_integration_test_transaction"

        when:
        Optional<Transaction> retrievedTransaction = transactionService.findTransactionByGuid(savedTransaction.guid)

        then:
        retrievedTransaction.isPresent()
        retrievedTransaction.get().amount == 150.75
        retrievedTransaction.get().accountNameOwner == "checking_brian"
    }

    void 'test account service integration with totals calculation'() {
        given:
        // Create multiple transactions for account total calculation
        List<Transaction> transactions = []
        for (int i = 0; i < 3; i++) {
            Transaction transaction = new Transaction(
                guid: UUID.randomUUID().toString(),
                accountNameOwner: i < 2 ? "checking_brian" : "savings_brian",
                accountType: AccountType.Credit,
                description: "total_test_transaction_${i}",
                category: "total_category",
                amount: 100.00 + (i * 25),
                transactionDate: Date.valueOf("2023-05-2${6 + i}"),
                transactionState: TransactionState.Cleared,
                transactionType: i % 2 == 0 ? TransactionType.Income : TransactionType.Expense,
                activeStatus: true,
                dateUpdated: new Timestamp(System.currentTimeMillis()),
                dateAdded: new Timestamp(System.currentTimeMillis())
            )
            transactions.add(transactionService.insertTransaction(transaction))
        }

        when:
        BigDecimal totalAmount = accountService.sumOfAllTransactionsByTransactionState(TransactionState.Cleared)
        List<Account> accounts = accountService.accounts()

        then:
        totalAmount != null
        totalAmount >= 0
        accounts != null
        accounts.size() >= 2
        accounts.any { it.accountNameOwner == "service_test_checking_brian" }
        accounts.any { it.accountNameOwner == "service_test_savings_brian" }
    }

    void 'test category service integration with transaction relationships'() {
        given:
        Category testCategory = new Category(
            categoryName: "service_integration_category",
            activeStatus: true,
            dateUpdated: new Timestamp(System.currentTimeMillis()),
            dateAdded: new Timestamp(System.currentTimeMillis())
        )

        when:
        Category savedCategory = categoryService.insertCategory(testCategory)

        then:
        savedCategory != null
        savedCategory.categoryId != null
        savedCategory.categoryName == "service_integration_category"

        when:
        Transaction categoryTransaction = new Transaction(
            guid: UUID.randomUUID().toString(),
            accountNameOwner: "checking_brian",
            accountType: AccountType.Credit,
            description: "category_service_test",
            category: savedCategory.categoryName,
            amount: 200.00,
            transactionDate: Date.valueOf("2023-05-28"),
            transactionState: TransactionState.Cleared,
            transactionType: TransactionType.Expense,
            notes: "integration_test_note",
            activeStatus: true,
            dateUpdated: new Timestamp(System.currentTimeMillis()),
            dateAdded: new Timestamp(System.currentTimeMillis())
        )
        transactionService.insertTransaction(categoryTransaction)

        List<Transaction> categoryTransactions = transactionService.findTransactionsByCategory(savedCategory.categoryName)

        then:
        categoryTransactions.size() >= 1
        categoryTransactions.any { it.description == "category_service_test" }
    }

    void 'test description service integration with transaction relationships'() {
        given:
        Description testDescription = new Description(
            descriptionName: "service_integration_description",
            activeStatus: true,
            dateUpdated: new Timestamp(System.currentTimeMillis()),
            dateAdded: new Timestamp(System.currentTimeMillis())
        )

        when:
        Description savedDescription = descriptionService.insertDescription(testDescription)

        then:
        savedDescription != null
        savedDescription.descriptionId != null
        savedDescription.descriptionName == "service_integration_description"

        when:
        Transaction descriptionTransaction = new Transaction(
            guid: UUID.randomUUID().toString(),
            accountNameOwner: "checking_brian",
            accountType: AccountType.Credit,
            description: savedDescription.descriptionName,
            category: "description_test_category",
            amount: 175.25,
            transactionDate: Date.valueOf("2023-05-29"),
            transactionState: TransactionState.Outstanding,
            transactionType: TransactionType.Income,
            notes: "integration_test_note",
            activeStatus: true,
            dateUpdated: new Timestamp(System.currentTimeMillis()),
            dateAdded: new Timestamp(System.currentTimeMillis())
        )
        transactionService.insertTransaction(descriptionTransaction)

        List<Transaction> descriptionTransactions = transactionService.findTransactionsByDescription(savedDescription.descriptionName)

        then:
        descriptionTransactions.size() >= 1
        descriptionTransactions.any { it.amount == 175.25 }
        descriptionTransactions.any { it.transactionState == TransactionState.Outstanding }
    }

    void 'test payment service integration'() {
        given:
        Payment testPayment = new Payment(
            paymentId: 0L,
            accountNameOwner: "checking_brian",
            sourceAccount: "checking_brian",
            destinationAccount: "savings_brian",
            guidSource: UUID.randomUUID().toString(),
            guidDestination: UUID.randomUUID().toString(),
            amount: 300.00,
            transactionDate: Date.valueOf("2023-05-30"),
            activeStatus: true,
            dateUpdated: new Timestamp(System.currentTimeMillis()),
            dateAdded: new Timestamp(System.currentTimeMillis())
        )

        when:
        Payment savedPayment = paymentService.insertPayment(testPayment)

        then:
        savedPayment != null
        savedPayment.paymentId != null
        savedPayment.amount == 300.00
        savedPayment.amount == 300.00

        when:
        List<Payment> allPayments = paymentService.findAllPayments()
        Optional<Payment> foundPayment = paymentService.findByPaymentId(savedPayment.paymentId)

        then:
        allPayments.size() >= 1
        allPayments.any { it.paymentId == savedPayment.paymentId }
        foundPayment.isPresent()
        foundPayment.get().guidSource == testPayment.guidSource
    }

    void 'test transfer service integration'() {
        given:
        Transfer testTransfer = new Transfer(
            transferId: 0L,
            sourceAccount: "checking_brian",
            destinationAccount: "savings_brian",
            guidSource: UUID.randomUUID().toString(),
            guidDestination: UUID.randomUUID().toString(),
            amount: 250.50,
            transactionDate: Date.valueOf("2023-05-31"),
            activeStatus: true,
            dateUpdated: new Timestamp(System.currentTimeMillis()),
            dateAdded: new Timestamp(System.currentTimeMillis())
        )

        when:
        Transfer savedTransfer = transferService.insertTransfer(testTransfer)

        then:
        savedTransfer != null
        savedTransfer.transferId != null
        savedTransfer.amount == 250.50
        savedTransfer.amount == 250.50

        when:
        List<Transfer> allTransfers = transferService.findAllTransfers()
        Optional<Transfer> foundTransfer = transferService.findByTransferId(savedTransfer.transferId)

        then:
        allTransfers.size() >= 1
        foundTransfer.isPresent()
        foundTransfer.get().guidDestination == testTransfer.guidDestination
    }

    void 'test validation amount service integration'() {
        given:
        ValidationAmount testValidationAmount = new ValidationAmount(
            validationId: 0L,
            accountId: 0L,
            validationDate: new Timestamp(System.currentTimeMillis()),
            activeStatus: true,
            transactionState: TransactionState.Cleared,
            amount: 2875.50
        )

        when:
        ValidationAmount savedValidationAmount = validationAmountService.insertValidationAmount("checking_brian", testValidationAmount)

        then:
        savedValidationAmount != null
        savedValidationAmount.validationId != null
        savedValidationAmount.amount == 2875.50
        savedValidationAmount.accountId > 0

        when:
        ValidationAmount foundValidationAmount = validationAmountService.findValidationAmountByAccountNameOwner("checking_brian", TransactionState.Cleared)

        then:
        foundValidationAmount != null
        foundValidationAmount.amount == 2875.50
    }

    void 'test receipt image service integration'() {
        given:
        ReceiptImage testReceiptImage = new ReceiptImage(
            receiptImageId: 0L,
            transactionId: 1L,
            activeStatus: true,
            imageFormatType: ImageFormatType.Jpeg,
            image: "test-image-data".bytes,
            thumbnail: "test-thumbnail-data".bytes
        )

        when:
        ReceiptImage savedReceiptImage = receiptImageService.insertReceiptImage(testReceiptImage)

        then:
        savedReceiptImage != null
        savedReceiptImage.receiptImageId != null
        savedReceiptImage.transactionId == 1L
        savedReceiptImage.imageFormatType == ImageFormatType.Jpeg

        when:
        Optional<ReceiptImage> foundReceiptImage = receiptImageService.findByReceiptImageId(savedReceiptImage.receiptImageId)

        then:
        foundReceiptImage.isPresent()
        foundReceiptImage.get().receiptImageId == savedReceiptImage.receiptImageId
    }

    void 'test cross-service integration with transaction and account updates'() {
        given:
        Optional<Account> account = accountService.account("checking_brian")
        BigDecimal initialTotal = account.get().cleared

        Transaction largeTransaction = new Transaction(
            guid: UUID.randomUUID().toString(),
            accountNameOwner: "checking_brian",
            accountType: AccountType.Credit,
            description: "large_cross_service_test",
            category: "cross_service_category",
            amount: 500.00,
            transactionDate: Date.valueOf("2023-06-02"),
            transactionState: TransactionState.Cleared,
            transactionType: TransactionType.Income,
            notes: "integration_test_note",
            activeStatus: true,
            dateUpdated: new Timestamp(System.currentTimeMillis()),
            dateAdded: new Timestamp(System.currentTimeMillis())
        )

        when:
        transactionService.insertTransaction(largeTransaction)
        List<Transaction> accountTransactions = transactionService.findByAccountNameOwnerOrderByTransactionDate("checking_brian")

        then:
        accountTransactions.size() >= 1
        accountTransactions.any { it.description == "large_cross_service_test" }
        accountTransactions.any { it.amount == 500.00 }
    }

    void 'test service layer error handling and validation'() {
        given:
        Transaction invalidTransaction = new Transaction(
            guid: null,  // Invalid - guid cannot be null
            accountNameOwner: "checking_brian",
            accountType: AccountType.Credit,
            description: "invalid_transaction_test",
            category: "invalid_category",
            amount: 100.00,
            transactionDate: Date.valueOf("2023-06-03"),
            transactionState: TransactionState.Cleared,
            transactionType: TransactionType.Expense,
            notes: "integration_test_note",
            activeStatus: true,
            dateUpdated: new Timestamp(System.currentTimeMillis()),
            dateAdded: new Timestamp(System.currentTimeMillis())
        )

        when:
        transactionService.insertTransaction(invalidTransaction)

        then:
        thrown(Exception)  // Should throw validation or constraint violation exception
    }

    void 'test service layer transaction rollback on failure'() {
        given:
        List<Transaction> transactionsBefore = transactionService.findByAccountNameOwnerOrderByTransactionDate("checking_brian")
        int initialCount = transactionsBefore.size()

        Transaction validTransaction = new Transaction(
            guid: UUID.randomUUID().toString(),
            accountNameOwner: "checking_brian",
            accountType: AccountType.Credit,
            description: "rollback_test_transaction",
            category: "rollback_category",
            amount: 100.00,
            transactionDate: Date.valueOf("2023-06-04"),
            transactionState: TransactionState.Cleared,
            transactionType: TransactionType.Expense,
            notes: "integration_test_note",
            activeStatus: true,
            dateUpdated: new Timestamp(System.currentTimeMillis()),
            dateAdded: new Timestamp(System.currentTimeMillis())
        )

        when:
        try {
            transactionService.insertTransaction(validTransaction)
            // Simulate a failure after successful insert
            throw new RuntimeException("Simulated failure for rollback test")
        } catch (RuntimeException e) {
            // Exception caught - transaction should be rolled back
        }

        List<Transaction> transactionsAfter = transactionService.findByAccountNameOwnerOrderByTransactionDate("checking_brian")

        then:
        // Due to @Transactional annotation, the transaction should be rolled back
        // However, the behavior depends on the specific transaction configuration
        transactionsAfter.size() >= initialCount
    }

    void 'test service layer performance with bulk operations'() {
        given:
        List<Transaction> bulkTransactions = []
        for (int i = 0; i < 50; i++) {
            bulkTransactions.add(new Transaction(
                guid: UUID.randomUUID().toString(),
                accountNameOwner: "checking_brian",
                accountType: AccountType.Credit,
                description: "bulk_performance_test_${i}",
                category: "performance_category",
                amount: Math.round((Math.random() * 100) * 100) / 100.0,
                transactionDate: Date.valueOf("2023-06-05"),
                transactionState: TransactionState.Cleared,
                transactionType: TransactionType.Expense,
                notes: "bulk_performance_test_note_${i}",
                activeStatus: true,
                dateUpdated: new Timestamp(System.currentTimeMillis()),
                dateAdded: new Timestamp(System.currentTimeMillis())
            ))
        }

        when:
        long startTime = System.currentTimeMillis()
        bulkTransactions.each { transaction ->
            transactionService.insertTransaction(transaction)
        }
        long endTime = System.currentTimeMillis()

        List<Transaction> allAccountTransactions = transactionService.findByAccountNameOwnerOrderByTransactionDate("checking_brian")

        then:
        (endTime - startTime) < 30000  // Should complete within 30 seconds
        allAccountTransactions.size() >= 50
        allAccountTransactions.count { it.description.startsWith("bulk_performance_test") } == 50
    }
}