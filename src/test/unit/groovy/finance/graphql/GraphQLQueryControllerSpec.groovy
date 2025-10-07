package finance.graphql

import finance.controllers.graphql.GraphQLQueryController
import finance.domain.Account
import finance.domain.AccountType
import finance.domain.Category
import finance.domain.Description
import finance.domain.Payment
import finance.domain.ServiceResult
import finance.domain.Transfer
import finance.services.BaseServiceSpec

import java.sql.Date
import java.sql.Timestamp

class GraphQLQueryControllerSpec extends BaseServiceSpec {

    def parameterServiceMock = GroovyMock(finance.services.StandardizedParameterService)
    def transferServiceMock = GroovyMock(finance.services.StandardizedTransferService)
    def receiptImageServiceMock = GroovyMock(finance.services.StandardizedReceiptImageService)
    def controller = new GraphQLQueryController(accountService, categoryService, descriptionService, parameterServiceMock, paymentService, transferServiceMock, receiptImageServiceMock)

    void setup() {
        accountService.meterService = meterService
        accountService.validator = validator
        categoryService.meterService = meterService
        categoryService.validator = validator
        descriptionService.meterService = meterService
        descriptionService.validator = validator
        paymentService.meterService = meterService
        paymentService.validator = validator
    }

    // Helper methods for creating test data
    private static Account makeAccount(Long id = 1L, String owner = 'checking_primary', AccountType type = AccountType.Checking) {
        new Account(
                id,
                owner,
                type,
                true,
                '1234',
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new Timestamp(0),
                new Timestamp(System.currentTimeMillis())
        )
    }

    private static Category makeCategory(Long id = 1L, String name = 'groceries') {
        new Category(
                id,
                true,
                name
        )
    }

    private static Description makeDescription(Long id = 1L, String name = 'walmart') {
        new Description(
                id,
                true,
                name
        )
    }

    private static Payment makePayment(Long id = 1L) {
        new Payment(
                id,
                'checking_primary',
                'bills_payable',
                new Date(System.currentTimeMillis()),
                BigDecimal.valueOf(100.00),
                'guid-source',
                'guid-dest',
                true
        )
    }

    private static Transfer makeTransfer(Long id = 1L) {
        new Transfer(
                id,
                'checking_primary',
                'savings_primary',
                new Date(System.currentTimeMillis()),
                BigDecimal.valueOf(200.00),
                'guid-source',
                'guid-dest',
                true
        )
    }

    // Account query tests
    def "accounts query returns list from service when no filter provided"() {
        given:
        def accounts = [makeAccount(1L, 'checking_primary'), makeAccount(2L, 'savings_primary')]
        accountRepositoryMock.findByActiveStatusOrderByAccountNameOwner(true) >> accounts

        expect:
        controller.accounts(null) == accounts
    }

    def "account query returns single account when present"() {
        given:
        def acct = makeAccount(42L, 'checking_primary')
        accountRepositoryMock.findByAccountNameOwner('checking_primary') >> Optional.of(acct)

        expect:
        controller.account('checking_primary') == acct
    }

    def "account query returns null when optional empty"() {
        given:
        accountRepositoryMock.findByAccountNameOwner('nonexistent') >> Optional.empty()

        expect:
        controller.account('nonexistent') == null
    }

    def "accounts query filters by accountType when provided"() {
        given:
        def checkingAccount = makeAccount(1L, 'checking_primary', AccountType.Checking)
        def debitAccount = makeAccount(2L, 'debit_primary', AccountType.Debit)
        def creditAccount = makeAccount(3L, 'credit_card', AccountType.Credit)

        accountRepositoryMock.findByActiveStatusAndAccountType(true, AccountType.Credit) >> [creditAccount]
        accountRepositoryMock.findByActiveStatusAndAccountType(true, AccountType.Debit) >> [debitAccount]
        accountRepositoryMock.findByActiveStatusAndAccountType(true, AccountType.Checking) >> [checkingAccount]

        when:
        def onlyCredit = controller.accounts(AccountType.Credit)
        def onlyDebit = controller.accounts(AccountType.Debit)
        def onlyChecking = controller.accounts(AccountType.Checking)

        then:
        onlyCredit*.accountType == [AccountType.Credit]
        onlyDebit*.accountType == [AccountType.Debit]
        onlyChecking*.accountType == [AccountType.Checking]
    }

    // Category query tests
    def "categories query returns list from service when successful"() {
        given:
        def categories = [makeCategory(1L, 'groceries'), makeCategory(2L, 'utilities')]
        categoryRepositoryMock.findByActiveStatusOrderByCategoryName(true) >> categories

        expect:
        controller.categories() == categories
    }

    def "categories query returns empty list when service fails"() {
        given:
        categoryRepositoryMock.findByActiveStatusOrderByCategoryName(true) >> { throw new RuntimeException("Database error") }

        expect:
        controller.categories() == []
    }

    def "category query returns single category when present"() {
        given:
        def category = makeCategory(42L, 'groceries')
        categoryRepositoryMock.findByCategoryName('groceries') >> Optional.of(category)

        expect:
        controller.category('groceries') == category
    }

    def "category query returns null when not found"() {
        given:
        categoryRepositoryMock.findByCategoryName('nonexistent') >> Optional.empty()

        expect:
        controller.category('nonexistent') == null
    }

    def "category query returns null when service fails"() {
        given:
        categoryRepositoryMock.findByCategoryName('groceries') >> { throw new RuntimeException("Database error") }

        expect:
        controller.category('groceries') == null
    }

    // Description query tests
    def "descriptions query returns list from service when successful"() {
        given:
        def descriptions = [makeDescription(1L, 'walmart'), makeDescription(2L, 'target')]
        descriptionRepositoryMock.findByActiveStatusOrderByDescriptionName(true) >> descriptions

        expect:
        controller.descriptions() == descriptions
    }

    def "descriptions query returns empty list when service fails"() {
        given:
        descriptionRepositoryMock.findByActiveStatusOrderByDescriptionName(true) >> { throw new RuntimeException("Database error") }

        expect:
        controller.descriptions() == []
    }

    def "description query returns single description when present"() {
        given:
        def description = makeDescription(42L, 'walmart')
        descriptionRepositoryMock.findByDescriptionName('walmart') >> Optional.of(description)

        expect:
        controller.description('walmart') == description
    }

    def "description query returns null when not found"() {
        given:
        descriptionRepositoryMock.findByDescriptionName('nonexistent') >> Optional.empty()

        expect:
        controller.description('nonexistent') == null
    }

    def "description query returns null when service fails"() {
        given:
        descriptionRepositoryMock.findByDescriptionName('walmart') >> { throw new RuntimeException("Database error") }

        expect:
        controller.description('walmart') == null
    }

    // Payment query tests
    def "payments query returns list from service"() {
        given:
        def payment1 = makePayment(1L)
        def payment2 = makePayment(2L)
        def payments = [payment1, payment2]
        paymentRepositoryMock.findAll() >> payments

        when:
        def result = controller.payments()

        then:
        result.size() == 2
        result.containsAll(payments)
    }

    def "payment query returns single payment when present"() {
        given:
        def payment = makePayment(42L)
        paymentRepositoryMock.findByPaymentId(42L) >> Optional.of(payment)

        expect:
        controller.payment(42L) == payment
    }

    def "payment query returns null when not found"() {
        given:
        paymentRepositoryMock.findByPaymentId(999L) >> Optional.empty()

        expect:
        controller.payment(999L) == null
    }

    // Transfer query tests
    def "transfers query returns list from service"() {
        given:
        def transfers = [makeTransfer(1L), makeTransfer(2L)]
        transferServiceMock.findAllTransfers() >> transfers

        expect:
        controller.transfers() == transfers
    }

    def "transfer query returns single transfer when present"() {
        given:
        def transfer = makeTransfer(42L)
        transferServiceMock.findByTransferId(42L) >> Optional.of(transfer)

        expect:
        controller.transfer(42L) == transfer
    }

    def "transfer query returns null when not found"() {
        given:
        transferServiceMock.findByTransferId(999L) >> Optional.empty()

        expect:
        controller.transfer(999L) == null
    }

    // Stub query tests
    def "transactions stub query returns empty list"() {
        expect:
        controller.transactions('checking_primary') == []
    }

    def "transaction stub query returns null"() {
        expect:
        controller.transaction(123L) == null
    }

    def "parameters stub query returns empty list"() {
        expect:
        controller.parameters() == []
    }

    def "parameter stub query returns null"() {
        expect:
        controller.parameter(123L) == null
    }

    def "validationAmounts stub query returns empty list"() {
        expect:
        controller.validationAmounts() == []
    }

    def "validationAmount stub query returns null"() {
        expect:
        controller.validationAmount(123L) == null
    }

    def "receiptImages stub query returns empty list"() {
        expect:
        controller.receiptImages() == []
    }

    def "receiptImage stub query returns null"() {
        expect:
        controller.receiptImage(123L) == null
    }
}
