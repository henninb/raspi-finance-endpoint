package finance.controllers

import finance.domain.Account
import finance.domain.Category
import finance.domain.Description
import finance.domain.Payment
import finance.domain.Transfer
import finance.services.AccountService
import finance.services.CategoryService
import finance.services.DescriptionService
import finance.services.PaymentService
import finance.services.TransferService
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class GraphQLQueryController(
    private val accountService: AccountService,
    private val categoryService: CategoryService,
    private val descriptionService: DescriptionService,
    private val paymentService: PaymentService,
    private val transferService: TransferService
) {

    companion object {
        val logger: Logger = LogManager.getLogger()
    }

    @QueryMapping
    fun accounts(): List<Account> {
        logger.info("GraphQL - Fetching all accounts")
        return accountService.accounts()
    }

    @QueryMapping
    fun account(@Argument accountNameOwner: String): Account? {
        logger.info("GraphQL - Fetching account: $accountNameOwner")
        return accountService.account(accountNameOwner).orElse(null)
    }

    @QueryMapping
    fun categories(): List<Category> {
        logger.info("GraphQL - Fetching all categories")
        return categoryService.categories()
    }

    @QueryMapping
    fun category(@Argument categoryName: String): Category? {
        logger.info("GraphQL - Fetching category: $categoryName")
        return categoryService.findByCategoryName(categoryName).orElse(null)
    }

    @QueryMapping
    fun descriptions(): List<Description> {
        logger.info("GraphQL - Fetching all descriptions")
        return descriptionService.fetchAllDescriptions()
    }

    @QueryMapping
    fun description(@Argument descriptionName: String): Description? {
        logger.info("GraphQL - Fetching description: $descriptionName")
        return descriptionService.findByDescriptionName(descriptionName).orElse(null)
    }

    @QueryMapping
    fun payments(): List<Payment> {
        logger.info("GraphQL - Fetching all payments")
        return paymentService.findAllPayments()
    }

    @QueryMapping
    fun payment(@Argument paymentId: Long): Payment? {
        logger.info("GraphQL - Fetching payment: $paymentId")
        return paymentService.findByPaymentId(paymentId).orElse(null)
    }

    @QueryMapping
    fun transfers(): List<Transfer> {
        logger.info("GraphQL - Fetching all transfers")
        return transferService.findAllTransfers()
    }

    @QueryMapping
    fun transfer(@Argument transferId: Long): Transfer? {
        logger.info("GraphQL - Fetching transfer: $transferId")
        return transferService.findByTransferId(transferId).orElse(null)
    }

    // Stub queries for remaining schema fields
    @QueryMapping
    fun transactions(@Argument accountNameOwner: String): List<Any> {
        logger.info("GraphQL - Fetching transactions for account: $accountNameOwner (stub)")
        return emptyList()
    }

    @QueryMapping
    fun transaction(@Argument transactionId: Long): Any? {
        logger.info("GraphQL - Fetching transaction: $transactionId (stub)")
        return null
    }

    @QueryMapping
    fun parameters(): List<Any> {
        logger.info("GraphQL - Fetching all parameters (stub)")
        return emptyList()
    }

    @QueryMapping
    fun parameter(@Argument parameterId: Long): Any? {
        logger.info("GraphQL - Fetching parameter: $parameterId (stub)")
        return null
    }

    @QueryMapping
    fun validationAmounts(): List<Any> {
        logger.info("GraphQL - Fetching all validation amounts (stub)")
        return emptyList()
    }

    @QueryMapping
    fun validationAmount(@Argument validationId: Long): Any? {
        logger.info("GraphQL - Fetching validation amount: $validationId (stub)")
        return null
    }

    @QueryMapping
    fun receiptImages(): List<Any> {
        logger.info("GraphQL - Fetching all receipt images (stub)")
        return emptyList()
    }

    @QueryMapping
    fun receiptImage(@Argument receiptImageId: Long): Any? {
        logger.info("GraphQL - Fetching receipt image: $receiptImageId (stub)")
        return null
    }
}