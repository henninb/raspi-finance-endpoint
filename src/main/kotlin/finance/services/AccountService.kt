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
        val account = accountRepository.findByAccountNameOwner(accountNameOwner)
        if (account.isPresent) {
            logger.info("Found account: $accountNameOwner")
        } else {
            logger.warn("Account not found: $accountNameOwner")
        }
        return account
    }

    @Timed
    override fun accounts(): List<Account> {
        val accounts = accountRepository.findByActiveStatusOrderByAccountNameOwner()
        if (accounts.isEmpty()) {
            logger.warn("findAllActiveAccounts - no accounts found.")
        } else {
            logger.info("findAllActiveAccounts - found accounts.")
        }
        return accounts
    }

    override fun findAccountsThatRequirePayment(): List<Account> {
        updateTotalsForAllAccounts()

        val accountsToInvestigate = accountRepository
            .findAccountsThatRequirePayment()

        // Log the count before filtering by Credit
        logger.info("Total accounts fetched: ${accountsToInvestigate.size}")

        return accountsToInvestigate
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
        throw RuntimeException("Account not inserted as the account already exists ${account.accountNameOwner}.")
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
}