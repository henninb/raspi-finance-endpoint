package finance.resolvers

import finance.domain.Account
import finance.domain.AccountType
import finance.services.AccountService
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
class AccountQueryResolver(private val accountService: AccountService) : GraphQLQueryResolver {

    @Suppress("unused")
    fun account(accountNameOwner: String) : Account {
        return accountService.findByAccountNameOwner(accountNameOwner).get()
    }

//    //TODO: should use a parameter 7/15/2021
//    @Suppress("unused")
//    fun accountsByType(accountType: AccountType) : List<Account> {
//        return accountService.findByActiveStatusAndAccountTypeAndTotalsIsGreaterThanOrderByAccountNameOwner()
//    }

    @Suppress("unused")
    fun accounts() : List<Account> {
        return accountService.findByActiveStatusOrderByAccountNameOwner()
    }
}