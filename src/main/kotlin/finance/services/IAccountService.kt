package finance.services

import finance.domain.Account
import finance.domain.TransactionState
import java.math.BigDecimal
import java.util.*

interface IAccountService {

    fun findByAccountNameOwner(accountNameOwner: String): Optional<Account>

    fun findByActiveStatusAndAccountTypeAndTotalsIsGreaterThanOrderByAccountNameOwner(): List<Account>

    fun findByActiveStatusOrderByAccountNameOwner(): List<Account>

    fun findAccountsThatRequirePayment(): List<String>

    fun sumOfAllTransactionsByTransactionState(transactionState: TransactionState): BigDecimal

    fun insertAccount(account: Account): Account

    fun deleteByAccountNameOwner(accountNameOwner: String): Boolean

    fun updateTotalsForAllAccounts(): Boolean

    fun updateAccount(account: Account): Account

    fun renameAccountNameOwner(oldAccountNameOwner: String, newAccountNameOwner: String): Account
}