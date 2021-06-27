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

    //fun computeTheGrandTotalForAllTransactions(): BigDecimal

    fun sumOfAllTransactionsByTransactionState(transactionState: TransactionState): BigDecimal

    fun insertAccount(account: Account): Boolean

    fun deleteByAccountNameOwner(accountNameOwner: String): Boolean

    fun updateTheGrandTotalsForAllAccounts(): Boolean

    fun updateAccount(account: Account): Boolean

    fun renameAccountNameOwner(oldAccountNameOwner: String, newAccountNameOwner: String): Boolean
}