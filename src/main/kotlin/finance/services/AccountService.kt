package finance.services

import finance.domain.Account
import finance.repositories.AccountRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*
import javax.validation.ConstraintViolation
import javax.validation.Validator

@Service
class AccountService @Autowired constructor(private var accountRepository: AccountRepository,
                                            private val validator: Validator) {
    private val logger = LoggerFactory.getLogger(this.javaClass)


    fun findByAccountNameOwner(accountNameOwner: String): Optional<Account> {
        return accountRepository.findByAccountNameOwner(accountNameOwner)
    }

    fun findAllActiveAccounts(): List<Account> {
        val accounts = accountRepository.findByActiveStatusOrderByAccountNameOwner(true)
        if (accounts.isEmpty()) {
            logger.warn("findAllActiveAccounts() - no accounts found.")
        } else {
            logger.info("findAllActiveAccounts() - found accounts.")
        }
        return accounts
    }

    fun selectTotals(): Double {
        return accountRepository.selectTotals()
    }

    fun selectTotalsCleared(): Double {
        return accountRepository.selectTotalsCleared()
    }

    fun insertAccount(account: Account): Boolean {
        val accountOptional = findByAccountNameOwner(account.accountNameOwner)
        val constraintViolations: Set<ConstraintViolation<Account>> = validator.validate(account)
        if (constraintViolations.isNotEmpty()) {
            //TODO: handle the violation
            logger.info("constraint issue.")
            return false
        }
        //TODO: Should saveAndFlush be in a try catch block?
        //logger.info("INFO: transactionRepository.saveAndFlush call.")
        if (!accountOptional.isPresent) {
            accountRepository.saveAndFlush(account)
        }
        //logger.info("INFO: transactionRepository.saveAndFlush success.")
        return true
    }

    fun deleteByAccountNameOwner(accountNameOwner: String) {
        accountRepository.deleteByAccountNameOwner(accountNameOwner)
    }

    fun updateAccountTotals() {
        logger.info("updateAccountGrandTotals")
        accountRepository.updateAccountGrandTotals()
        logger.info("updateAccountClearedTotals")
        accountRepository.updateAccountClearedTotals()
        logger.info("updateAccountTotals")
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