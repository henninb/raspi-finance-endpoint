package finance.resolvers

import finance.domain.Account
import finance.services.IAccountService
import graphql.schema.DataFetcher
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.stereotype.Component

@Component
class AccountGraphQLResolver(
    private val accountService: IAccountService,
) {

    val accounts: DataFetcher<List<Account>>
        get() = DataFetcher { environment ->
            try {
                logger.info("GraphQL: Fetching all active accounts")
                val accounts = accountService.accounts()
                logger.info("GraphQL: Successfully fetched ${accounts.size} accounts")

                // Log sample account data for debugging serialization
                if (accounts.isNotEmpty()) {
                    val firstAccount = accounts.first()
                    logger.debug("GraphQL: Sample account data - ID: ${firstAccount.accountId}, Name: ${firstAccount.accountNameOwner}, Type: ${firstAccount.accountType}")
                    logger.debug("GraphQL: Sample account timestamps - dateAdded: ${firstAccount.dateAdded}, dateUpdated: ${firstAccount.dateUpdated}")

                    // Test serialization
                    try {
                        val serialized = firstAccount.toString()
                        logger.debug("GraphQL: Account serialization test successful, length: ${serialized.length}")
                    } catch (serializationException: Exception) {
                        logger.error("GraphQL: Account serialization failed", serializationException)
                    }
                }

                accounts
            } catch (e: Exception) {
                logger.error("GraphQL: Error fetching accounts", e)
                throw e
            }
        }

    fun account(): DataFetcher<Account?> {
        return DataFetcher { environment ->
            val name: String = requireNotNull(environment.getArgument<String>("accountNameOwner")) { "accountNameOwner is required" }
            logger.info("GraphQL: Fetching account: $name")
            accountService.account(name).orElse(null)
        }
    }

    companion object {
        val logger: Logger = LogManager.getLogger()
    }
}

