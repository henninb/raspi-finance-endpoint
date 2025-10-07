package finance.controllers.graphql

import finance.domain.Account
import finance.domain.AccountType
import finance.domain.Category
import finance.domain.Description
import finance.domain.Parameter
import finance.domain.Payment
import finance.domain.ReceiptImage
import finance.domain.ServiceResult
import finance.domain.Transaction
import finance.domain.Transfer
import finance.services.StandardizedAccountService
import finance.services.StandardizedCategoryService
import finance.services.StandardizedDescriptionService
import finance.services.StandardizedParameterService
import finance.services.StandardizedPaymentService
import finance.services.StandardizedReceiptImageService
import finance.services.StandardizedTransferService
import spock.lang.Specification

class GraphQLQueryControllerSpec extends Specification {

    GraphQLQueryController controller
    StandardizedAccountService mockAccountService
    StandardizedCategoryService mockCategoryService
    StandardizedDescriptionService mockDescriptionService
    StandardizedParameterService mockParameterService
    StandardizedPaymentService mockPaymentService
    StandardizedTransferService mockTransferService
    StandardizedReceiptImageService mockReceiptImageService

    def setup() {
        mockAccountService = Mock(StandardizedAccountService)
        mockCategoryService = Mock(StandardizedCategoryService)
        mockDescriptionService = Mock(StandardizedDescriptionService)
        mockParameterService = Mock(StandardizedParameterService)
        mockPaymentService = Mock(StandardizedPaymentService)
        mockTransferService = Mock(StandardizedTransferService)
        mockReceiptImageService = Mock(StandardizedReceiptImageService)

        controller = new GraphQLQueryController(
            mockAccountService,
            mockCategoryService,
            mockDescriptionService,
            mockParameterService,
            mockPaymentService,
            mockTransferService,
            mockReceiptImageService
        )
    }

    def "accounts should return all accounts when accountType is null"() {
        given: "a list of all accounts"
        def allAccounts = [
            new Account(accountNameOwner: "checking_primary", accountType: AccountType.Checking),
            new Account(accountNameOwner: "savings_primary", accountType: AccountType.Savings),
            new Account(accountNameOwner: "credit_primary", accountType: AccountType.CreditCard)
        ]

        when: "accounts is called with null accountType"
        def result = controller.accounts(null)

        then: "service returns all accounts"
        1 * mockAccountService.accounts() >> allAccounts

        and: "all accounts are returned"
        result == allAccounts
        result.size() == 3
    }

    def "accounts should return filtered accounts when accountType is provided"() {
        given: "a list of checking accounts"
        def checkingAccounts = [
            new Account(accountNameOwner: "checking_primary", accountType: AccountType.Checking),
            new Account(accountNameOwner: "checking_secondary", accountType: AccountType.Checking)
        ]

        when: "accounts is called with Checking accountType"
        def result = controller.accounts(AccountType.Checking)

        then: "service returns filtered accounts"
        1 * mockAccountService.accountsByType(AccountType.Checking) >> checkingAccounts

        and: "only checking accounts are returned"
        result == checkingAccounts
        result.size() == 2
        result.every { it.accountType == AccountType.Checking }
    }

    def "account should return account by name"() {
        given: "an account"
        def account = new Account(accountNameOwner: "checking_primary", accountType: AccountType.Checking)

        when: "account is called with accountNameOwner"
        def result = controller.account("checking_primary")

        then: "service returns the account"
        1 * mockAccountService.account("checking_primary") >> Optional.of(account)

        and: "account is returned"
        result == account
        result.accountNameOwner == "checking_primary"
    }

    def "account should return null when account not found"() {
        when: "account is called with non-existent accountNameOwner"
        def result = controller.account("nonexistent_account")

        then: "service returns empty optional"
        1 * mockAccountService.account("nonexistent_account") >> Optional.empty()

        and: "null is returned"
        result == null
    }

    def "categories should return all active categories"() {
        given: "a list of categories"
        def categories = [
            new Category(categoryName: "groceries"),
            new Category(categoryName: "utilities")
        ]

        when: "categories is called"
        def result = controller.categories()

        then: "service returns all active categories"
        1 * mockCategoryService.findAllActive() >> ServiceResult.Success.of(categories)

        and: "categories are returned"
        result == categories
        result.size() == 2
    }

    def "categories should return empty list when service fails"() {
        when: "categories is called"
        def result = controller.categories()

        then: "service returns not found"
        1 * mockCategoryService.findAllActive() >> ServiceResult.NotFound.of("error")

        and: "empty list is returned"
        result == []
    }

    def "category should return category by name"() {
        given: "a category"
        def category = new Category(categoryName: "groceries")

        when: "category is called with categoryName"
        def result = controller.category("groceries")

        then: "service returns the category"
        1 * mockCategoryService.findByCategoryNameStandardized("groceries") >> ServiceResult.Success.of(category)

        and: "category is returned"
        result == category
        result.categoryName == "groceries"
    }

    def "category should return null when category not found"() {
        when: "category is called with non-existent categoryName"
        def result = controller.category("nonexistent")

        then: "service returns not found"
        1 * mockCategoryService.findByCategoryNameStandardized("nonexistent") >> ServiceResult.NotFound.of("not found")

        and: "null is returned"
        result == null
    }

    def "descriptions should return all active descriptions"() {
        given: "a list of descriptions"
        def descriptions = [
            new Description(descriptionName: "amazon"),
            new Description(descriptionName: "walmart")
        ]

        when: "descriptions is called"
        def result = controller.descriptions()

        then: "service returns all active descriptions"
        1 * mockDescriptionService.findAllActive() >> ServiceResult.Success.of(descriptions)

        and: "descriptions are returned"
        result == descriptions
        result.size() == 2
    }

    def "descriptions should return empty list when service fails"() {
        when: "descriptions is called"
        def result = controller.descriptions()

        then: "service returns not found"
        1 * mockDescriptionService.findAllActive() >> ServiceResult.NotFound.of("error")

        and: "empty list is returned"
        result == []
    }

    def "description should return description by name"() {
        given: "a description"
        def description = new Description(descriptionName: "amazon")

        when: "description is called with descriptionName"
        def result = controller.description("amazon")

        then: "service returns the description"
        1 * mockDescriptionService.findByDescriptionNameStandardized("amazon") >> ServiceResult.Success.of(description)

        and: "description is returned"
        result == description
        result.descriptionName == "amazon"
    }

    def "description should return null when description not found"() {
        when: "description is called with non-existent descriptionName"
        def result = controller.description("nonexistent")

        then: "service returns not found"
        1 * mockDescriptionService.findByDescriptionNameStandardized("nonexistent") >> ServiceResult.NotFound.of("not found")

        and: "null is returned"
        result == null
    }

    def "payments should return all payments"() {
        given: "a list of payments"
        def payments = [
            new Payment(paymentId: 1L, sourceAccount: "checking_primary"),
            new Payment(paymentId: 2L, sourceAccount: "savings_primary")
        ]

        when: "payments is called"
        def result = controller.payments()

        then: "service returns all payments"
        1 * mockPaymentService.findAllPayments() >> payments

        and: "payments are returned"
        result == payments
        result.size() == 2
    }

    def "payment should return payment by ID"() {
        given: "a payment"
        def payment = new Payment(paymentId: 123L, sourceAccount: "checking_primary")

        when: "payment is called with paymentId"
        def result = controller.payment(123L)

        then: "service returns the payment"
        1 * mockPaymentService.findByPaymentId(123L) >> Optional.of(payment)

        and: "payment is returned"
        result == payment
        result.paymentId == 123L
    }

    def "payment should return null when payment not found"() {
        when: "payment is called with non-existent paymentId"
        def result = controller.payment(999L)

        then: "service returns empty optional"
        1 * mockPaymentService.findByPaymentId(999L) >> Optional.empty()

        and: "null is returned"
        result == null
    }

    def "transfers should return all transfers"() {
        given: "a list of transfers"
        def transfers = [
            new Transfer(transferId: 1L, sourceAccount: "checking_primary"),
            new Transfer(transferId: 2L, sourceAccount: "savings_primary")
        ]

        when: "transfers is called"
        def result = controller.transfers()

        then: "service returns all transfers"
        1 * mockTransferService.findAllTransfers() >> transfers

        and: "transfers are returned"
        result == transfers
        result.size() == 2
    }

    def "transfer should return transfer by ID"() {
        given: "a transfer"
        def transfer = new Transfer(transferId: 321L, sourceAccount: "checking_primary")

        when: "transfer is called with transferId"
        def result = controller.transfer(321L)

        then: "service returns the transfer"
        1 * mockTransferService.findByTransferId(321L) >> Optional.of(transfer)

        and: "transfer is returned"
        result == transfer
        result.transferId == 321L
    }

    def "transfer should return null when transfer not found"() {
        when: "transfer is called with non-existent transferId"
        def result = controller.transfer(999L)

        then: "service returns empty optional"
        1 * mockTransferService.findByTransferId(999L) >> Optional.empty()

        and: "null is returned"
        result == null
    }

    def "transactions should return empty list (stub)"() {
        when: "transactions is called"
        def result = controller.transactions("checking_primary")

        then: "empty list is returned"
        result == []
    }

    def "transaction should return null (stub)"() {
        when: "transaction is called"
        def result = controller.transaction(123L)

        then: "null is returned"
        result == null
    }

    def "parameters should return list from service when successful"() {
        given: "a list of parameters"
        def parameters = [
            new Parameter(1L, "param1", "value1", true),
            new Parameter(2L, "param2", "value2", true)
        ]

        when: "parameters is called"
        def result = controller.parameters()

        then: "service returns success result"
        1 * mockParameterService.findAllActive() >> ServiceResult.Success.of(parameters)

        and: "parameter list is returned"
        result == parameters
        result.size() == 2
    }

    def "parameters should return empty list when service fails"() {
        when: "parameters is called"
        def result = controller.parameters()

        then: "service returns failure result"
        1 * mockParameterService.findAllActive() >> ServiceResult.NotFound.of("No parameters found")

        and: "empty list is returned"
        result == []
    }

    def "parameter should return single parameter when present"() {
        given: "a parameter"
        def parameter = new Parameter(123L, "test_param", "test_value", true)

        when: "parameter is called"
        def result = controller.parameter(123L)

        then: "service returns success result"
        1 * mockParameterService.findById(123L) >> ServiceResult.Success.of(parameter)

        and: "parameter is returned"
        result == parameter
        result.parameterId == 123L
        result.parameterName == "test_param"
    }

    def "parameter should return null when not found"() {
        when: "parameter is called"
        def result = controller.parameter(123L)

        then: "service returns not found result"
        1 * mockParameterService.findById(123L) >> ServiceResult.NotFound.of("Parameter not found")

        and: "null is returned"
        result == null
    }

    def "parameter should return null when service fails"() {
        when: "parameter is called"
        def result = controller.parameter(123L)

        then: "service returns system error"
        1 * mockParameterService.findById(123L) >> ServiceResult.SystemError.of(new RuntimeException("DB Error"))

        and: "null is returned"
        result == null
    }

    def "validationAmounts should return empty list (stub)"() {
        when: "validationAmounts is called"
        def result = controller.validationAmounts()

        then: "empty list is returned"
        result == []
    }

    def "validationAmount should return null (stub)"() {
        when: "validationAmount is called"
        def result = controller.validationAmount(123L)

        then: "null is returned"
        result == null
    }

    def "receiptImages should return empty list (stub)"() {
        when: "receiptImages is called"
        def result = controller.receiptImages()

        then: "empty list is returned"
        result == []
    }

    def "receiptImage should return null (stub)"() {
        when: "receiptImage is called"
        def result = controller.receiptImage(123L)

        then: "null is returned"
        result == null
    }

    def "transactionReceiptImage should return receipt image when found"() {
        given: "a transaction and receipt image"
        def transaction = new Transaction(transactionId: 123L)
        def receiptImage = new ReceiptImage(receiptImageId: 456L, transactionId: 123L)

        when: "transactionReceiptImage is called"
        def result = controller.transactionReceiptImage(transaction)

        then: "service returns the receipt image"
        1 * mockReceiptImageService.findByTransactionId(123L) >> ServiceResult.Success.of(receiptImage)

        and: "receipt image is returned"
        result == receiptImage
        result.receiptImageId == 456L
    }

    def "transactionReceiptImage should return null when not found"() {
        given: "a transaction without receipt image"
        def transaction = new Transaction(transactionId: 123L)

        when: "transactionReceiptImage is called"
        def result = controller.transactionReceiptImage(transaction)

        then: "service returns not found"
        1 * mockReceiptImageService.findByTransactionId(123L) >> ServiceResult.NotFound.of("not found")

        and: "null is returned"
        result == null
    }
}
