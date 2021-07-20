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
import org.springframework.stereotype.Component
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
            val descriptions = descriptionService.fetchAllDescriptions()
            descriptions
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
            val categoryName : String = it.getArgument("category")
            logger.info(categoryName)
            val category = Category()
            category.category = categoryName

            categoryService.insertCategory(category)
        }
    }

    fun createDescription(): DataFetcher<Description> {
        return DataFetcher<Description> {
            val descriptionName : String = it.getArgument("description")
            logger.info(description)
            val description = Description()
            description.description = descriptionName

            descriptionService.insertDescription(description)
        }
    }

    val accounts: DataFetcher<List<Account>>
        get() = DataFetcher<List<Account>> {
            val accounts = accountService.findByActiveStatusOrderByAccountNameOwner()
            accounts
        }

    fun account(): DataFetcher<Account> {
        return DataFetcher { accountService.findByAccountNameOwner(it.getArgument("accountNameOwner")).get()
        }
    }

    val categories: DataFetcher<List<Category>>
        get() = DataFetcher<List<Category>> {
            val list: List<Category> = categoryService.fetchAllActiveCategories()
            list
        }

    fun payment(): DataFetcher<Payment> {
        return DataFetcher { paymentService.findByPaymentId(it.getArgument("paymentId")).get()
            }
    }

    companion object {
        val mapper = ObjectMapper()
        val logger: Logger = LogManager.getLogger()
    }
}