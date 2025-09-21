package finance.controllers

import finance.domain.Account
import finance.services.IAccountService
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

/**
 * Modern Spring Boot 4.0 GraphQL Account Controller
 * Uses annotation-based approach with automatic discovery and wiring
 */
@Controller
open class AccountGraphQLController(
    private val accountService: IAccountService
) {

    @QueryMapping
    fun accounts(): List<Account> {
        return try {
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
                    logger.debug("GraphQL: Account serialization test successful: ${serialized.length} chars")
                } catch (e: Exception) {
                    logger.warn("GraphQL: Account serialization test failed", e)
                }
            }

            accounts
        } catch (e: Exception) {
            logger.error("GraphQL: Error fetching accounts", e)
            throw e
        }
    }

    @QueryMapping
    fun account(@Argument accountNameOwner: String): Account? {
        return try {
            logger.info("GraphQL: Fetching account with name: $accountNameOwner")
            val accountOptional = accountService.account(accountNameOwner)

            if (accountOptional.isPresent) {
                logger.info("GraphQL: Successfully fetched account: $accountNameOwner")
                accountOptional.get()
            } else {
                logger.warn("GraphQL: Account not found: $accountNameOwner")
                null
            }
        } catch (e: Exception) {
            logger.error("GraphQL: Error fetching account", e)
            throw e
        }
    }

    companion object {
        val logger: Logger = LogManager.getLogger()
    }
}