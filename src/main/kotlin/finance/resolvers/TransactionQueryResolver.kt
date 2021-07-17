package finance.resolvers

import finance.domain.Account
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component
import finance.domain.Transaction
import finance.services.TransactionService

@Component
class TransactionQueryResolver(private val transactionService: TransactionService) : GraphQLQueryResolver {

    @Suppress("unused")
    fun transactions(accountNameOwner: String) : List<Transaction> {
        return transactionService.findByAccountNameOwnerOrderByTransactionDate(accountNameOwner)
    }
}