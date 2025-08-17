package finance.services

import finance.domain.Account
import finance.domain.AccountType
import finance.domain.TransactionState
import finance.repositories.AccountRepository
import io.micrometer.core.annotation.Timed
import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import jakarta.validation.ConstraintViolation
import org.springframework.dao.InvalidDataAccessResourceUsageException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Timestamp
import java.util.*


@Service
open class AccountService(
    private var accountRepository: AccountRepository,
) : IAccountService, BaseService() {

    @Timed
    override fun account(accountNameOwner: String): Optional<Account> {
        logger.info("Finding account: $accountNameOwner")
        return executeWithResilienceSync(
            operation = {
                accountRepository.findByAccountNameOwner(accountNameOwner)
            },
            operationName = "findAccountByName-$accountNameOwner"
        ).also { account ->
            if (account.isPresent) {
                logger.info("Found account: $accountNameOwner")
            } else {
                logger.warn("Account not found: $accountNameOwner")
            }
        }
    }

    @Timed
    override fun accounts(): List<Account> {
        return executeWithResilienceSync(
            operation = {
                accountRepository.findByActiveStatusOrderByAccountNameOwner()
            },
            operationName = "findAllActiveAccounts"
        ).also { accounts ->
            if (accounts.isEmpty()) {
                logger.warn("findAllActiveAccounts - no accounts found.")
            } else {
                logger.info("findAllActiveAccounts - found accounts.")
            }
        }
    }

    override fun findAccountsThatRequirePayment(): List<Account> {
        updateTotalsForAllAccounts()

        return executeWithResilienceSync(
            operation = {
                accountRepository.findAccountsThatRequirePayment()
            },
            operationName = "findAccountsThatRequirePayment"
        ).also { accountsToInvestigate ->
            logger.info("Total accounts fetched: ${accountsToInvestigate.size}")
        }
    }

    @Timed
    override fun sumOfAllTransactionsByTransactionState(transactionState: TransactionState): BigDecimal {
        val totals: BigDecimal = accountRepository.sumOfAllTransactionsByTransactionState(transactionState.toString())
        return totals.setScale(2, RoundingMode.HALF_UP)
    }

    @Timed
    override fun insertAccount(account: Account): Account {
        val accountOptional = account(account.accountNameOwner)
        val constraintViolations: Set<ConstraintViolation<Account>> = validator.validate(account)
        handleConstraintViolations(constraintViolations, meterService)

        if (!accountOptional.isPresent) {
            logger.info("Inserting new account: ${account.accountNameOwner}")
            val timestamp = Timestamp(System.currentTimeMillis())
            account.dateAdded = timestamp
            account.dateUpdated = timestamp
            val savedAccount = accountRepository.saveAndFlush(account)
            logger.info("Successfully inserted account: ${savedAccount.accountNameOwner} with ID: ${savedAccount.accountId}")
            return savedAccount
        }
        logger.error("Account not inserted as the account already exists ${account.accountNameOwner}.")
        throw org.springframework.dao.DataIntegrityViolationException("Account not inserted as the account already exists ${account.accountNameOwner}.")
    }

    @Timed
    override fun deleteAccount(accountNameOwner: String): Boolean {
        val accountOptional = accountRepository.findByAccountNameOwner(accountNameOwner)
        if (!accountOptional.isPresent) {
            logger.warn("Account not found: $accountNameOwner")
            return false
        }
        accountRepository.delete(accountOptional.get())
        return true
    }

    @Timed
    override fun updateTotalsForAllAccounts(): Boolean {
        try {
            accountRepository.updateTotalsForAllAccounts()
        } catch (invalidDataAccessResourceUsageException: InvalidDataAccessResourceUsageException) {
            meterService.incrementExceptionCaughtCounter("InvalidDataAccessResourceUsageException")
            logger.warn("InvalidDataAccessResourceUsageException: ${invalidDataAccessResourceUsageException.message}")
        }
        return true
    }

    @Timed
    override fun updateAccount(account: Account): Account {
        val optionalAccount = accountRepository.findByAccountId(account.accountId)
        if (optionalAccount.isPresent) {
            val accountToBeUpdated = optionalAccount.get()
            account.dateUpdated = Timestamp(System.currentTimeMillis())
            logger.info("Updating account: ${accountToBeUpdated.accountId} - ${accountToBeUpdated.accountNameOwner}")
            val updatedAccount = accountRepository.saveAndFlush(account)
            logger.info("Successfully updated account: ${updatedAccount.accountNameOwner}")
            return updatedAccount
        }
        throw RuntimeException("Account not updated as the account does not exists ${account.accountNameOwner}.")
    }

    @Transactional
    @Timed
    override fun renameAccountNameOwner(oldAccountNameOwner: String, newAccountNameOwner: String): Account {
        val oldAccount = accountRepository.findByAccountNameOwner(oldAccountNameOwner)
            .orElseThrow { EntityNotFoundException("Account not found") }

        logger.info("Renaming account from $oldAccountNameOwner to $newAccountNameOwner")
        oldAccount.accountNameOwner = newAccountNameOwner
        oldAccount.dateUpdated = Timestamp(System.currentTimeMillis())
        val renamedAccount = accountRepository.saveAndFlush(oldAccount)
        logger.info("Successfully renamed account to: $newAccountNameOwner")
        return renamedAccount
    }

    @Timed
    override fun deactivateAccount(accountNameOwner: String): Account {
        try {
            return executeWithResilienceSync(
                operation = {
                    val account = accountRepository.findByAccountNameOwner(accountNameOwner)
                        .orElseThrow { EntityNotFoundException("Account not found: $accountNameOwner") }

                    logger.info("Deactivating account: $accountNameOwner")
                    account.activeStatus = false
                    account.dateUpdated = Timestamp(System.currentTimeMillis())
                    val updatedAccount = accountRepository.saveAndFlush(account)
                    logger.info("Successfully deactivated account: $accountNameOwner")
                    updatedAccount
                },
                operationName = "deactivateAccount-$accountNameOwner"
            )
        } catch (ex: java.util.concurrent.ExecutionException) {
            // Unwrap the actual exception from ExecutionException
            val cause = ex.cause
            if (cause is EntityNotFoundException) {
                throw cause
            } else {
                throw ex
            }
        } catch (ex: org.springframework.dao.DataAccessResourceFailureException) {
            // Check if the wrapped exception is EntityNotFoundException
            var currentCause = ex.cause
            while (currentCause != null) {
                if (currentCause is EntityNotFoundException) {
                    throw currentCause
                }
                currentCause = currentCause.cause
            }
            throw ex
        }
    }

    @Timed
    override fun activateAccount(accountNameOwner: String): Account {
        try {
            return executeWithResilienceSync(
                operation = {
                    val account = accountRepository.findByAccountNameOwner(accountNameOwner)
                        .orElseThrow { EntityNotFoundException("Account not found: $accountNameOwner") }

                    logger.info("Activating account: $accountNameOwner")
                    account.activeStatus = true
                    account.dateUpdated = Timestamp(System.currentTimeMillis())
                    val updatedAccount = accountRepository.saveAndFlush(account)
                    logger.info("Successfully activated account: $accountNameOwner")
                    updatedAccount
                },
                operationName = "activateAccount-$accountNameOwner"
            )
        } catch (ex: java.util.concurrent.ExecutionException) {
            // Unwrap the actual exception from ExecutionException
            val cause = ex.cause
            if (cause is EntityNotFoundException) {
                throw cause
            } else {
                throw ex
            }
        } catch (ex: org.springframework.dao.DataAccessResourceFailureException) {
            // Check if the wrapped exception is EntityNotFoundException
            var currentCause = ex.cause
            while (currentCause != null) {
                if (currentCause is EntityNotFoundException) {
                    throw currentCause
                }
                currentCause = currentCause.cause
            }
            throw ex
        }
    }
}