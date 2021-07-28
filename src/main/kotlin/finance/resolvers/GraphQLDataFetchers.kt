package finance.resolvers

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Account
import finance.domain.Category
import finance.domain.Description
import finance.domain.Payment
import finance.services.AccountService
import finance.services.CategoryService
import finance.services.DescriptionService
import finance.services.PaymentService
import graphql.schema.DataFetcher
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import scala.sys.*
import java.sql.Date
import java.util.*


@Component
class GraphQLDataFetchers(
    private val descriptionService: DescriptionService,
    private val accountService: AccountService,
    private val categoryService: CategoryService,
    private val paymentService: PaymentService
) {
    val descriptions: DataFetcher<List<Description>>
        get() = DataFetcher<List<Description>> {
            val username = SecurityContextHolder.getContext().authentication.name
            val authentication: Authentication = SecurityContextHolder.getContext().authentication
            logger.info(authentication.isAuthenticated)
            logger.info(username)
           // val page: String = it.getArgument("page")
           //     .orElse(AppConstants.DEFAULT_PAGE_NUMBER) as String?. toInt ()
           // val size: Int =
           //     Optional.ofNullable(env.getArgument("size")).orElse(AppConstants.DEFAULT_PAGE_SIZE) as String?. toInt ()
            return@DataFetcher descriptionService.fetchAllDescriptions()
        }

    val description: DataFetcher<Description>
        get() = DataFetcher<Description> {
            val description = descriptionService.findByDescriptionName(it.getArgument("descriptionName")).get()
            description
        }

//    @GqlDataFetcher(type = GqlType.MUTATION)
//    fun postBoard(): DataFetcher<*>? {
//        return DataFetcher<Any> { environment: DataFetchingEnvironment ->
//            val entity = BoardEntity()
//            entity.update(environment.arguments)
//            boardRepository.save(entity)
//        }
//    }

    fun createPayment(): DataFetcher<Payment> {
        return DataFetcher<Payment> {
            val raw = it.arguments["payment"]
            val paymentInput = mapper.convertValue(raw, Payment::class.java)
            paymentInput.transactionDate = Date.valueOf("2022-01-01")
            paymentInput.guidSource = UUID.randomUUID().toString()
            paymentInput.guidDestination = UUID.randomUUID().toString()
            logger.debug(paymentInput.toString())
            val paymentResponse = paymentService.insertPayment(paymentInput)
            paymentResponse
        }
    }

    fun createCategory(): DataFetcher<Category> {
        return DataFetcher<Category> {
            val categoryName: String = it.getArgument("category")
            logger.info(categoryName)
            val category = Category()
            category.category = categoryName

            categoryService.insertCategory(category)
        }
    }

    fun createDescription(): DataFetcher<Description> {
        return DataFetcher<Description> {
            val descriptionName: String = it.getArgument("description")
            logger.info(description)
            val description = Description()
            description.description = descriptionName

            descriptionService.insertDescription(description)
        }
    }

    val payments: DataFetcher<List<Payment>>
        get() = DataFetcher<List<Payment>> {
            return@DataFetcher paymentService.findAllPayments()
        }

    val accounts: DataFetcher<List<Account>>
        get() = DataFetcher<List<Account>> {
            return@DataFetcher accountService.findByActiveStatusOrderByAccountNameOwner()
        }

    fun account(): DataFetcher<Account> {
        return DataFetcher {
            accountService.findByAccountNameOwner(it.getArgument("accountNameOwner")).get()
        }
    }

    val categories: DataFetcher<List<Category>>
        get() = DataFetcher<List<Category>> {
            return@DataFetcher categoryService.fetchAllActiveCategories()
        }

    fun payment(): DataFetcher<Payment> {
        return DataFetcher {
            paymentService.findByPaymentId(it.getArgument("paymentId")).get()
        }
    }

    companion object {
        val mapper = ObjectMapper()
        val logger: Logger = LogManager.getLogger()
    }
}