package finance.services

import finance.domain.Account
import finance.repositories.AccountRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.InvalidDataAccessResourceUsageException
import org.springframework.stereotype.Service
import java.util.*
import javax.validation.ConstraintViolation
import javax.validation.ValidationException
import javax.validation.Validator

@Service
class AccountService @Autowired constructor(private var accountRepository: AccountRepository,
                                            private val validator: Validator) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun findByAccountNameOwner(accountNameOwner: String): Optional<Account> {
        return accountRepository.findByAccountNameOwner(accountNameOwner)
    }

    fun findByActiveStatusOrderByAccountNameOwner(): List<Account> {
        val accounts = accountRepository.findByActiveStatusOrderByAccountNameOwner(true)
        if (accounts.isEmpty()) {
            logger.warn("findAllActiveAccounts() - no accounts found.")
        } else {
            logger.info("findAllActiveAccounts() - found accounts.")
        }
        return accounts
    }

    fun findAccountsThatRequirePayment(): List<String> {
        return accountRepository.findAccountsThatRequirePayment()
    }

    fun computeTheGrandTotalForAllTransactions(): Double {
        val totals: Double
        try {
            totals = accountRepository.computeTheGrandTotalForAllTransactions()
        } catch (e: Exception) {
            return 0.0
        }
        return totals
    }

    fun computeTheGrandTotalForAllClearedTransactions(): Double {
        val totals: Double
        try {
            totals = accountRepository.computeTheGrandTotalForAllClearedTransactions()
        } catch (e: Exception) {
            return 0.0
        }
        return totals
    }

    fun insertAccount(account: Account): Boolean {
        val accountOptional = findByAccountNameOwner(account.accountNameOwner)
        val constraintViolations: Set<ConstraintViolation<Account>> = validator.validate(account)
        if (constraintViolations.isNotEmpty()) {
            logger.error("Cannot insert account as there is a constraint violation on the data.")
            throw ValidationException("Cannot insert account as there is a constraint violation on the data.")
        }

        if (!accountOptional.isPresent) {
            accountRepository.saveAndFlush(account)
        }
        //logger.info("INFO: transactionRepository.saveAndFlush success.")
        return true
    }

    fun deleteByAccountNameOwner(accountNameOwner: String) {
        accountRepository.deleteByAccountNameOwner(accountNameOwner)
    }

    fun updateTheGrandTotalForAllClearedTransactions() {
        try {
            logger.info("updateAccountGrandTotals")
            accountRepository.updateTheGrandTotalForAllClearedTransactions()
            logger.info("updateAccountClearedTotals")
            accountRepository.updateTheGrandTotalForAllTransactions()
            logger.info("updateAccountTotals")
        } catch (sqlGrammarException: InvalidDataAccessResourceUsageException) {
            logger.info("empty database.")
        }
    }

    //TODO: Complete the function
    fun patchAccount(account: Account): Boolean {
        val optionalAccount = accountRepository.findByAccountNameOwner(account.accountNameOwner)
        if (optionalAccount.isPresent) {
            logger.info("patch the account.")
            //var updateFlag = false
            //val fromDb = optionalAccount.get()
        }

        return false
    }
}