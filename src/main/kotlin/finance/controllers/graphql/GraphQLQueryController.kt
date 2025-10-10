package finance.controllers.graphql

import finance.domain.Account
import finance.domain.AccountType
import finance.domain.Category
import finance.domain.ClaimStatus
import finance.domain.Description
import finance.domain.MedicalExpense
import finance.domain.Parameter
import finance.domain.Payment
import finance.domain.ReceiptImage
import finance.domain.ServiceResult
import finance.domain.Transaction
import finance.domain.Transfer
import finance.domain.ValidationAmount
import finance.services.StandardizedAccountService
import finance.services.StandardizedCategoryService
import finance.services.StandardizedDescriptionService
import finance.services.StandardizedMedicalExpenseService
import finance.services.StandardizedParameterService
import finance.services.StandardizedPaymentService
import finance.services.StandardizedReceiptImageService
import finance.services.StandardizedTransferService
import finance.services.StandardizedValidationAmountService
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class GraphQLQueryController(
    private val accountService: StandardizedAccountService,
    private val categoryService: StandardizedCategoryService,
    private val descriptionService: StandardizedDescriptionService,
    private val medicalExpenseService: StandardizedMedicalExpenseService,
    private val parameterService: StandardizedParameterService,
    private val paymentService: StandardizedPaymentService,
    private val transferService: StandardizedTransferService,
    private val receiptImageService: StandardizedReceiptImageService,
    private val validationAmountService: StandardizedValidationAmountService,
) {
    companion object {
        val logger: Logger = LogManager.getLogger()
    }

    @QueryMapping(name = "accounts")
    fun accounts(
        @Argument accountType: AccountType?,
    ): List<Account> =
        if (accountType == null) {
            logger.info("GraphQL - Fetching all accounts (unfiltered)")
            accountService.accounts()
        } else {
            logger.info("GraphQL - Fetching accounts filtered by type: {}", accountType)
            accountService.accountsByType(accountType)
        }

    @QueryMapping
    fun account(
        @Argument accountNameOwner: String,
    ): Account? {
        logger.info("GraphQL - Fetching account: $accountNameOwner")
        return accountService.account(accountNameOwner).orElse(null)
    }

    @QueryMapping
    fun categories(): List<Category> {
        logger.info("GraphQL - Fetching all categories")
        return when (val result = categoryService.findAllActive()) {
            is ServiceResult.Success -> result.data
            else -> emptyList()
        }
    }

    @QueryMapping
    fun category(
        @Argument categoryName: String,
    ): Category? {
        logger.info("GraphQL - Fetching category: $categoryName")
        return when (val result = categoryService.findByCategoryNameStandardized(categoryName)) {
            is ServiceResult.Success -> result.data
            else -> null
        }
    }

    @QueryMapping
    fun descriptions(): List<Description> {
        logger.info("GraphQL - Fetching all descriptions")
        return when (val result = descriptionService.findAllActive()) {
            is ServiceResult.Success -> result.data
            else -> emptyList()
        }
    }

    @QueryMapping
    fun description(
        @Argument descriptionName: String,
    ): Description? {
        logger.info("GraphQL - Fetching description: $descriptionName")
        return when (val result = descriptionService.findByDescriptionNameStandardized(descriptionName)) {
            is ServiceResult.Success -> result.data
            else -> null
        }
    }

    @QueryMapping
    fun payments(): List<Payment> {
        logger.info("GraphQL - Fetching all payments")
        return paymentService.findAllPayments()
    }

    @QueryMapping
    fun payment(
        @Argument paymentId: Long,
    ): Payment? {
        logger.info("GraphQL - Fetching payment: $paymentId")
        return paymentService.findByPaymentId(paymentId).orElse(null)
    }

    @QueryMapping
    fun transfers(): List<Transfer> {
        logger.info("GraphQL - Fetching all transfers")
        return transferService.findAllTransfers()
    }

    @QueryMapping
    fun transfer(
        @Argument transferId: Long,
    ): Transfer? {
        logger.info("GraphQL - Fetching transfer: $transferId")
        return transferService.findByTransferId(transferId).orElse(null)
    }

    // Stub queries for remaining schema fields
    @QueryMapping
    fun transactions(
        @Argument accountNameOwner: String,
    ): List<Any> {
        logger.info("GraphQL - Fetching transactions for account: $accountNameOwner (stub)")
        return emptyList()
    }

    @QueryMapping
    fun transaction(
        @Argument transactionId: Long,
    ): Any? {
        logger.info("GraphQL - Fetching transaction: $transactionId (stub)")
        return null
    }

    @QueryMapping
    fun parameters(): List<Parameter> {
        logger.info("GraphQL - Fetching all parameters")
        return when (val result = parameterService.findAllActive()) {
            is ServiceResult.Success -> result.data
            else -> emptyList()
        }
    }

    @QueryMapping
    fun parameter(
        @Argument parameterId: Long,
    ): Parameter? {
        logger.info("GraphQL - Fetching parameter: $parameterId")
        return when (val result = parameterService.findById(parameterId)) {
            is ServiceResult.Success -> result.data
            else -> null
        }
    }

    @QueryMapping
    fun validationAmounts(): List<ValidationAmount> {
        logger.info("GraphQL - Fetching all validation amounts")
        return when (val result = validationAmountService.findAllActive()) {
            is ServiceResult.Success -> result.data
            else -> emptyList()
        }
    }

    @QueryMapping
    fun validationAmount(
        @Argument validationId: Long,
    ): ValidationAmount? {
        logger.info("GraphQL - Fetching validation amount: $validationId")
        return when (val result = validationAmountService.findById(validationId)) {
            is ServiceResult.Success -> result.data
            else -> null
        }
    }

    @QueryMapping
    fun receiptImages(): List<Any> {
        logger.info("GraphQL - Fetching all receipt images (stub)")
        return emptyList()
    }

    @QueryMapping
    fun receiptImage(
        @Argument receiptImageId: Long,
    ): Any? {
        logger.info("GraphQL - Fetching receipt image: $receiptImageId (stub)")
        return null
    }

    @QueryMapping
    fun medicalExpenses(): List<MedicalExpense> {
        logger.info("GraphQL - Fetching all medical expenses")
        return when (val result = medicalExpenseService.findAllActive()) {
            is ServiceResult.Success -> result.data
            else -> emptyList()
        }
    }

    @QueryMapping
    fun medicalExpense(
        @Argument medicalExpenseId: Long,
    ): MedicalExpense? {
        logger.info("GraphQL - Fetching medical expense: $medicalExpenseId")
        return when (val result = medicalExpenseService.findById(medicalExpenseId)) {
            is ServiceResult.Success -> result.data
            else -> null
        }
    }

    @QueryMapping
    fun medicalExpensesByClaimStatus(
        @Argument claimStatus: ClaimStatus,
    ): List<MedicalExpense> {
        logger.info("GraphQL - Fetching medical expenses by claim status: $claimStatus")
        return medicalExpenseService.findMedicalExpensesByClaimStatus(claimStatus)
    }

    /**
     * Field resolver for Transaction.receiptImage - follows centralized GraphQL architecture
     * Replaces the previous TransactionBatchResolver with proper schema mapping
     */
    @SchemaMapping(typeName = "Transaction", field = "receiptImage")
    fun transactionReceiptImage(transaction: Transaction): ReceiptImage? {
        logger.debug("GraphQL - Resolving receiptImage for transaction: {}", transaction.transactionId)
        return when (val result = receiptImageService.findByTransactionId(transaction.transactionId)) {
            is ServiceResult.Success -> {
                logger.debug("GraphQL - Found receiptImage for transaction: {}", transaction.transactionId)
                result.data
            }
            else -> {
                logger.debug("GraphQL - No receiptImage found for transaction: {}", transaction.transactionId)
                null
            }
        }
    }
}
