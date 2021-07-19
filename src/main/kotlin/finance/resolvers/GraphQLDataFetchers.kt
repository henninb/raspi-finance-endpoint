package finance.resolvers

import finance.domain.Account
import finance.domain.Category
import finance.domain.Description
import finance.domain.Payment
import finance.services.AccountService
import finance.services.CategoryService
import finance.services.DescriptionService
import finance.services.PaymentService
import graphql.schema.DataFetcher
import org.springframework.stereotype.Component

@Component
class GraphQLDataFetchers(
    private val descriptionService: DescriptionService,
    private val accountService: AccountService,
    private val categoryService: CategoryService,
    private val paymentService: PaymentService
) {
    val descriptions: DataFetcher<List<Description>>
        get() = DataFetcher<List<Description>> {
            val list: List<Description> = descriptionService.fetchAllDescriptions()
            list
        }

    val accounts: DataFetcher<List<Account>>
        get() = DataFetcher<List<Account>> {
            val list: List<Account> = accountService.findByActiveStatusOrderByAccountNameOwner()
            list
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

    fun account(): DataFetcher<Account> {
        return DataFetcher { accountService.findByAccountNameOwner(it.getArgument("accountNameOwner")).get()
        }
    }
}