package finance.services

import finance.models.Account
import finance.repositories.AccountRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
open class AccountService {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    @Autowired
    private lateinit var accountRepository: AccountRepository<Account>

    fun findAllOrderByAccountNameOwner(): List<Account> {
        return accountRepository.findAll()
    }

    fun findAllAcitveAccounts(): List<Account> {
        val accounts = accountRepository.findByActiveStatusOrderByAccountNameOwner("Y")
        if( accounts.isEmpty()) {
            logger.warn("findAllAcitveAccounts() problem.")
        } else {
            logger.info("findAllAcitveAccounts()")
        }
        return accounts
    }

    fun findByAccountNameOwner(accountNameOwner: String): Optional<Account> {
        return accountRepository.findByAccountNameOwner(accountNameOwner)
    }

    fun insertAccount(account: Account) : Boolean {
        //TODO: Should saveAndFlush be in a try catch block?
        accountRepository.saveAndFlush(account)
        logger.info("INFO: transactionRepository.saveAndFlush success.")
        return true
    }

    fun deleteByAccountNameOwner(accountNameOwner: String) {
        accountRepository.deleteByAccountNameOwner(accountNameOwner)
    }

    //TODO: Complete the function
    fun patchAccount(account: Account) : Boolean {
        val optionalAccount = accountRepository.findByAccountNameOwner(account.accountNameOwner.toString())
        if ( optionalAccount.isPresent ) {
            var updateFlag = false
            val fromDb = optionalAccount.get()
        }

        return false
    }
}