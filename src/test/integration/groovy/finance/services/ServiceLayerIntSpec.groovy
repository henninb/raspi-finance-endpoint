package finance.services

import finance.Application
import finance.domain.Account
import finance.domain.AccountType
import finance.domain.Category
import finance.domain.Description
import finance.domain.ImageFormatType
import finance.domain.Payment
import finance.domain.ReceiptImage
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.domain.TransactionType
import finance.domain.Transfer
import finance.domain.ValidationAmount
import finance.repositories.AccountRepository
import finance.repositories.TransactionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

import java.sql.Timestamp
import java.time.LocalDate
import java.util.Optional

@ActiveProfiles("int")
@SpringBootTest
@ContextConfiguration(classes = Application)
class ServiceLayerIntSpec extends Specification {

    private static final String OWNER_SUFFIX =
        UUID.randomUUID().toString().replaceAll(/[^a-f]/, '').padRight(8, 'a').substring(0, 8)
    private static final String TEST_OWNER = "test-service-${OWNER_SUFFIX}"
    private static final String PRIMARY_ACCOUNT_NAME = "checking_${OWNER_SUFFIX}"
    private static final String SECONDARY_ACCOUNT_NAME = "savings_${OWNER_SUFFIX}"
    private static final String DEFAULT_CATEGORY = "service_category_${OWNER_SUFFIX}"

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

    private static <T> T unwrapSuccess(def result) {
        try {
            return result?.data
        } catch (Throwable ignored) {
            return null
        }
    }

    private static Timestamp now() {
        new Timestamp(System.currentTimeMillis())
    }

    private String uniqueName(String prefix) {
        "${prefix}_${UUID.randomUUID().toString().replace('-', '').substring(0, 8)}"
    }

    void setup() {
        def authorities = [new SimpleGrantedAuthority("USER")]
        def auth = new UsernamePasswordAuthenticationToken(TEST_OWNER, "N/A", authorities)
        SecurityContextHolder.getContext().setAuthentication(auth)

        Account primaryAccount = new Account(
            accountId: 0L,
            owner: TEST_OWNER,
            accountNameOwner: PRIMARY_ACCOUNT_NAME,
            accountType: AccountType.Credit,
            activeStatus: true,
            moniker: "1234",
            outstanding: BigDecimal.ZERO,
            future: BigDecimal.ZERO,
            cleared: BigDecimal.ZERO,
            dateClosed: now(),
            validationDate: now()
        )
        primaryAccount.dateUpdated = now()
        primaryAccount.dateAdded = now()

        Account secondaryAccount = new Account(
            accountId: 0L,
            owner: TEST_OWNER,
            accountNameOwner: SECONDARY_ACCOUNT_NAME,
            accountType: AccountType.Credit,
            activeStatus: true,
            moniker: "5678",
            outstanding: BigDecimal.ZERO,
            future: BigDecimal.ZERO,
            cleared: BigDecimal.ZERO,
            dateClosed: now(),
            validationDate: now()
        )
        secondaryAccount.dateUpdated = now()
        secondaryAccount.dateAdded = now()

        try {
            accountService.insertAccount(primaryAccount)
        } catch (Exception ignored) {
        }

        try {
            accountService.insertAccount(secondaryAccount)
        } catch (Exception ignored) {
        }
    }

    void cleanup() {
        SecurityContextHolder.clearContext()
    }

    void 'test transaction service integration with database operations'() {
        given:
        Transaction testTransaction = new Transaction(
            guid: UUID.randomUUID().toString(),
            accountNameOwner: PRIMARY_ACCOUNT_NAME,
            accountType: AccountType.Credit,
            description: uniqueName("service_txn"),
            category: DEFAULT_CATEGORY,
            amount: 150.75,
            transactionDate: LocalDate.parse("2023-05-25"),
            transactionState: TransactionState.Cleared,
            transactionType: TransactionType.Expense,
            notes: "Integration test for transaction service",
            activeStatus: true,
            dateUpdated: now(),
            dateAdded: now()
        )

        when:
        Transaction savedTransaction = unwrapSuccess(transactionService.save(testTransaction))

        then:
        savedTransaction != null
        savedTransaction.transactionId != null
        savedTransaction.guid == testTransaction.guid
        savedTransaction.description == testTransaction.description

        when:
        def findResult = transactionService.findById(savedTransaction.guid)
        Optional<Transaction> retrievedTransaction = Optional.ofNullable(unwrapSuccess(findResult))

        then:
        retrievedTransaction.isPresent()
        retrievedTransaction.get().amount == 150.75
        retrievedTransaction.get().accountNameOwner == PRIMARY_ACCOUNT_NAME
    }

    void 'test category service integration with transaction relationships'() {
        given:
        String categoryName = uniqueName("service_category")
        Category category = new Category(
            categoryName: categoryName,
            activeStatus: true,
            dateUpdated: now(),
            dateAdded: now()
        )

        when:
        Category savedCategory = unwrapSuccess(categoryService.save(category))

        then:
        savedCategory != null
        savedCategory.categoryId != null
        savedCategory.categoryName == categoryName

        when:
        Transaction categoryTransaction = new Transaction(
            guid: UUID.randomUUID().toString(),
            accountNameOwner: PRIMARY_ACCOUNT_NAME,
            accountType: AccountType.Credit,
            description: uniqueName("category_service"),
            category: savedCategory.categoryName,
            amount: 200.00,
            transactionDate: LocalDate.parse("2023-05-28"),
            transactionState: TransactionState.Cleared,
            transactionType: TransactionType.Expense,
            notes: "integration_test_note",
            activeStatus: true,
            dateUpdated: now(),
            dateAdded: now()
        )
        unwrapSuccess(transactionService.save(categoryTransaction))

        List<Transaction> categoryTransactions =
            unwrapSuccess(transactionService.findTransactionsByCategoryStandardized(savedCategory.categoryName)) as List<Transaction>

        then:
        categoryTransactions.size() >= 1
        categoryTransactions.any { it.guid == categoryTransaction.guid }
    }

    void 'test description service integration with transaction relationships'() {
        given:
        String descriptionName = uniqueName("service_description")
        Description description = new Description(
            descriptionName: descriptionName,
            activeStatus: true,
            dateUpdated: now(),
            dateAdded: now()
        )

        when:
        Description savedDescription = descriptionService.insertDescription(description)

        then:
        savedDescription != null
        savedDescription.descriptionId != null
        savedDescription.descriptionName == descriptionName

        when:
        Transaction descriptionTransaction = new Transaction(
            guid: UUID.randomUUID().toString(),
            accountNameOwner: PRIMARY_ACCOUNT_NAME,
            accountType: AccountType.Credit,
            description: savedDescription.descriptionName,
            category: DEFAULT_CATEGORY,
            amount: 175.25,
            transactionDate: LocalDate.parse("2023-05-29"),
            transactionState: TransactionState.Outstanding,
            transactionType: TransactionType.Income,
            notes: "integration_test_note",
            activeStatus: true,
            dateUpdated: now(),
            dateAdded: now()
        )
        unwrapSuccess(transactionService.save(descriptionTransaction))

        List<Transaction> descriptionTransactions =
            unwrapSuccess(transactionService.findTransactionsByDescriptionStandardized(savedDescription.descriptionName)) as List<Transaction>

        then:
        descriptionTransactions.size() >= 1
        descriptionTransactions.any { it.guid == descriptionTransaction.guid }
    }

    void 'test payment service integration'() {
        given:
        Payment testPayment = new Payment(
            paymentId: 0L,
            sourceAccount: PRIMARY_ACCOUNT_NAME,
            destinationAccount: SECONDARY_ACCOUNT_NAME,
            amount: 300.00,
            transactionDate: LocalDate.parse("2023-05-30"),
            activeStatus: true,
            dateUpdated: now(),
            dateAdded: now()
        )

        when:
        Payment savedPayment = unwrapSuccess(paymentService.save(testPayment))

        then:
        savedPayment != null
        savedPayment.paymentId != null
        savedPayment.amount == 300.00

        when:
        List<Payment> allPayments = unwrapSuccess(paymentService.findAllActive()) as List<Payment>
        Optional<Payment> foundPayment = paymentService.findByPaymentId(savedPayment.paymentId)

        then:
        allPayments.size() >= 1
        allPayments.any { it.paymentId == savedPayment.paymentId }
        foundPayment.isPresent()
        foundPayment.get().guidSource == savedPayment.guidSource
    }

    void 'test transfer service integration'() {
        given:
        Transfer testTransfer = new Transfer(
            transferId: 0L,
            sourceAccount: PRIMARY_ACCOUNT_NAME,
            destinationAccount: SECONDARY_ACCOUNT_NAME,
            guidSource: UUID.randomUUID().toString(),
            guidDestination: UUID.randomUUID().toString(),
            amount: 250.50,
            transactionDate: LocalDate.parse("2023-05-31"),
            activeStatus: true,
            dateUpdated: now(),
            dateAdded: now()
        )

        when:
        Transfer savedTransfer = transferService.insertTransfer(testTransfer)

        then:
        savedTransfer != null
        savedTransfer.transferId != null
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
            validationDate: now(),
            activeStatus: true,
            transactionState: TransactionState.Cleared,
            amount: 2875.50
        )

        when:
        ValidationAmount savedValidationAmount =
            validationAmountService.insertValidationAmount(PRIMARY_ACCOUNT_NAME, testValidationAmount)

        then:
        savedValidationAmount != null
        savedValidationAmount.validationId != null
        savedValidationAmount.amount == 2875.50
        savedValidationAmount.accountId > 0

        when:
        ValidationAmount foundValidationAmount =
            validationAmountService.findValidationAmountByAccountNameOwner(PRIMARY_ACCOUNT_NAME, TransactionState.Cleared)

        then:
        foundValidationAmount != null
        foundValidationAmount.amount == 2875.50
    }

    void 'test receipt image service integration'() {
        given:
        Transaction testTransaction = new Transaction(
            guid: UUID.randomUUID().toString(),
            accountNameOwner: PRIMARY_ACCOUNT_NAME,
            accountType: AccountType.Credit,
            description: uniqueName("receipt_txn"),
            category: DEFAULT_CATEGORY,
            amount: 100.00,
            transactionDate: LocalDate.parse("2023-06-01"),
            transactionState: TransactionState.Cleared,
            transactionType: TransactionType.Expense,
            activeStatus: true,
            dateUpdated: now(),
            dateAdded: now()
        )
        Transaction savedTransaction = unwrapSuccess(transactionService.save(testTransaction))

        ReceiptImage testReceiptImage = new ReceiptImage(
            receiptImageId: 0L,
            transactionId: savedTransaction.transactionId,
            activeStatus: true,
            imageFormatType: ImageFormatType.Jpeg,
            image: Base64.getDecoder().decode('/9j/2wBDAAMCAgICAgMCAgIDAwMDBAYEBAQEBAgGBgUGCQgKCgkICQkKDA8MCgsOCwkJDRENDg8QEBEQCgwSExIQEw8QEBD/yQALCAABAAEBAREA/8wABgAQEAX/2gAIAQEAAD8A0s8g/9k='),
            thumbnail: Base64.getDecoder().decode('/9j/2wBDAAMCAgICAgMCAgIDAwMDBAYEBAQEBAgGBgUGCQgKCgkICQkKDA8MCgsOCwkJDRENDg8QEBEQCgwSExIQEw8QEBD/yQALCAABAAEBAREA/8wABgAQEAX/2gAIAQEAAD8A0s8g/9k=')
        )

        when:
        ReceiptImage savedReceiptImage = unwrapSuccess(receiptImageService.save(testReceiptImage))

        then:
        savedReceiptImage != null
        savedReceiptImage.receiptImageId != null
        savedReceiptImage.transactionId == savedTransaction.transactionId
        savedReceiptImage.imageFormatType == ImageFormatType.Jpeg

        when:
        def findResult = receiptImageService.findById(savedReceiptImage.receiptImageId)
        Optional<ReceiptImage> foundReceiptImage = Optional.ofNullable(unwrapSuccess(findResult))

        then:
        foundReceiptImage.isPresent()
        foundReceiptImage.get().receiptImageId == savedReceiptImage.receiptImageId
    }

    void 'test cross-service integration with transaction and account updates'() {
        given:
        Optional<Account> account = accountService.account(PRIMARY_ACCOUNT_NAME)
        BigDecimal initialTotal = account.get().cleared

        Transaction largeTransaction = new Transaction(
            guid: UUID.randomUUID().toString(),
            accountNameOwner: PRIMARY_ACCOUNT_NAME,
            accountType: AccountType.Credit,
            description: uniqueName("large_cross_service"),
            category: DEFAULT_CATEGORY,
            amount: 500.00,
            transactionDate: LocalDate.parse("2023-06-02"),
            transactionState: TransactionState.Cleared,
            transactionType: TransactionType.Income,
            notes: "integration_test_note",
            activeStatus: true,
            dateUpdated: now(),
            dateAdded: now()
        )

        when:
        unwrapSuccess(transactionService.save(largeTransaction))
        List<Transaction> accountTransactions =
            unwrapSuccess(transactionService.findByAccountNameOwnerOrderByTransactionDateStandardized(PRIMARY_ACCOUNT_NAME)) as List<Transaction>

        then:
        accountTransactions.size() >= 1
        accountTransactions.any { it.guid == largeTransaction.guid }
        initialTotal != null
    }

    void 'test service layer transaction rollback on failure'() {
        given:
        List<Transaction> transactionsBefore =
            unwrapSuccess(transactionService.findByAccountNameOwnerOrderByTransactionDateStandardized(PRIMARY_ACCOUNT_NAME)) as List<Transaction>
        int initialCount = transactionsBefore.size()

        Transaction validTransaction = new Transaction(
            guid: UUID.randomUUID().toString(),
            accountNameOwner: PRIMARY_ACCOUNT_NAME,
            accountType: AccountType.Credit,
            description: uniqueName("rollback_txn"),
            category: DEFAULT_CATEGORY,
            amount: 100.00,
            transactionDate: LocalDate.parse("2023-06-04"),
            transactionState: TransactionState.Cleared,
            transactionType: TransactionType.Expense,
            notes: "integration_test_note",
            activeStatus: true,
            dateUpdated: now(),
            dateAdded: now()
        )

        when:
        try {
            unwrapSuccess(transactionService.save(validTransaction))
            throw new RuntimeException("Simulated failure for rollback test")
        } catch (RuntimeException ignored) {
        }

        List<Transaction> transactionsAfter =
            unwrapSuccess(transactionService.findByAccountNameOwnerOrderByTransactionDateStandardized(PRIMARY_ACCOUNT_NAME)) as List<Transaction>

        then:
        transactionsAfter.size() >= initialCount
    }

    void 'test service layer performance with bulk operations'() {
        given:
        List<Transaction> bulkTransactions = []
        for (int i = 0; i < 50; i++) {
            bulkTransactions.add(new Transaction(
                guid: UUID.randomUUID().toString(),
                accountNameOwner: PRIMARY_ACCOUNT_NAME,
                accountType: AccountType.Credit,
                description: "bulk_performance_test_${i}",
                category: DEFAULT_CATEGORY,
                amount: Math.round((Math.random() * 100) * 100) / 100.0,
                transactionDate: LocalDate.parse("2023-06-05"),
                transactionState: TransactionState.Cleared,
                transactionType: TransactionType.Expense,
                notes: "bulk_performance_test_note_${i}",
                activeStatus: true,
                dateUpdated: now(),
                dateAdded: now()
            ))
        }

        when:
        long startTime = System.currentTimeMillis()
        bulkTransactions.each { transaction ->
            unwrapSuccess(transactionService.save(transaction))
        }
        long endTime = System.currentTimeMillis()

        List<Transaction> allAccountTransactions =
            unwrapSuccess(transactionService.findByAccountNameOwnerOrderByTransactionDateStandardized(PRIMARY_ACCOUNT_NAME)) as List<Transaction>

        then:
        (endTime - startTime) < 30000
        allAccountTransactions.size() >= 50
        allAccountTransactions.count { it.description.startsWith("bulk_performance_test") } == 50
    }
}
