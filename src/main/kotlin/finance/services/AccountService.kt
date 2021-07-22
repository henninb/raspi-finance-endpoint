package finance.services

import finance.domain.Account
import finance.domain.TransactionState
import finance.repositories.AccountRepository
import finance.repositories.TransactionRepository
import io.micrometer.core.annotation.Timed
import org.springframework.dao.InvalidDataAccessResourceUsageException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Timestamp
import java.util.*
import javax.validation.ConstraintViolation

@Service
open class AccountService(
    private var accountRepository: AccountRepository,
    private var transactionRepository: TransactionRepository
) : IAccountService, BaseService() {

    @Timed
    override fun findByAccountNameOwner(accountNameOwner: String): Optional<Account> {
        return accountRepository.findByAccountNameOwner(accountNameOwner)
    }

    @Timed
    override fun findByActiveStatusAndAccountTypeAndTotalsIsGreaterThanOrderByAccountNameOwner(): List<Account> {
        val accounts = accountRepository.findByActiveStatusAndAccountTypeOrderByAccountNameOwner()

        if (accounts.isEmpty()) {
            logger.warn("findAllActiveAccounts - no accounts found.")
        } else {
            logger.info("findAllActiveAccounts - found accounts.")
        }
        return accounts.filter { a ->
            (a.outstanding > BigDecimal(0.0) || a.future > BigDecimal(0.0) || a.cleared > BigDecimal(
                0.0
            ))
        }
    }

    @Timed
    override fun findByActiveStatusOrderByAccountNameOwner(): List<Account> {
        val accounts = accountRepository.findByActiveStatusOrderByAccountNameOwner()
        if (accounts.isEmpty()) {
            logger.warn("findAllActiveAccounts - no accounts found.")
        } else {
            logger.info("findAllActiveAccounts - found accounts.")
        }
        return accounts
    }

    //TODO: Should return a list of account?
    @Timed
    override fun findAccountsThatRequirePayment(): List<String> {
        return accountRepository.findAccountsThatRequirePayment()
    }

    @Timed
    override fun sumOfAllTransactionsByTransactionState(transactionState: TransactionState): BigDecimal {
        val totals: BigDecimal = accountRepository.sumOfAllTransactionsByTransactionState(transactionState.toString())
        return totals.setScale(2, RoundingMode.HALF_UP)
    }

    @Timed
    override fun insertAccount(account: Account): Account {
        val accountOptional = findByAccountNameOwner(account.accountNameOwner)
        val constraintViolations: Set<ConstraintViolation<Account>> = validator.validate(account)
        handleConstraintViolations(constraintViolations, meterService)

        if (!accountOptional.isPresent) {
            account.dateAdded = Timestamp(Calendar.getInstance().time.time)
            account.dateUpdated = Timestamp(Calendar.getInstance().time.time)
            return accountRepository.saveAndFlush(account)
        }
        logger.error("Account not inserted as the account already exists ${account.accountNameOwner}.")
        throw RuntimeException("Account not inserted as the account already exists ${account.accountNameOwner}.")
    }

    @Timed
    override fun deleteByAccountNameOwner(accountNameOwner: String): Boolean {
        val account = accountRepository.findByAccountNameOwner(accountNameOwner).get()
        accountRepository.delete(account)
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

    //TODO: 6/24/2021 - Complete the method logic
    @Timed
    override fun updateAccount(account: Account): Account {
        val optionalAccount = accountRepository.findByAccountNameOwner(account.accountNameOwner)
        if (optionalAccount.isPresent) {
            val accountToBeUpdated = optionalAccount.get()
            //account.dateUpdated = Timestamp(Calendar.getInstance().time.time)
            logger.info("updated the account.")
            //var updateFlag = false
            //val fromDb = optionalAccount.get()
            return accountRepository.saveAndFlush(accountToBeUpdated)
        }
        throw RuntimeException("Account not updated as the account does not exists ${account.accountNameOwner}.")
    }

    @Timed
    override fun renameAccountNameOwner(oldAccountNameOwner: String, newAccountNameOwner: String): Account {
        val newAccountOptional = accountRepository.findByAccountNameOwner(newAccountNameOwner)
        val oldAccountOptional = accountRepository.findByAccountNameOwner(oldAccountNameOwner)

        if (!oldAccountOptional.isPresent) {
            throw RuntimeException("Cannot find the original account to rename: $oldAccountNameOwner")
        }
        if (newAccountOptional.isPresent) {
            throw RuntimeException("Cannot overwrite new account with an existing account : $newAccountNameOwner")
        }
        val oldAccount = oldAccountOptional.get()
        val newAccount = Account()
        newAccount.accountType = oldAccount.accountType
        newAccount.activeStatus = oldAccount.activeStatus
        newAccount.moniker = oldAccount.moniker
        newAccount.accountNameOwner = newAccountNameOwner
        val newlySavedAccount = accountRepository.saveAndFlush(newAccount)

        val transactions =
            transactionRepository.findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc(oldAccountNameOwner)
        transactions.forEach { transaction ->
            transaction.accountNameOwner = newlySavedAccount.accountNameOwner
            transaction.accountId = newlySavedAccount.accountId
            transaction.accountType = newlySavedAccount.accountType
            transaction.activeStatus = newlySavedAccount.activeStatus
            transactionRepository.saveAndFlush(transaction)
        }
        accountRepository.delete(oldAccountOptional.get())
        return newlySavedAccount
    }
}