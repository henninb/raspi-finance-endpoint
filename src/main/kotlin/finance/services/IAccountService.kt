package finance.services

import finance.domain.Account
import finance.domain.TransactionState
import java.math.BigDecimal
import java.util.*

interface IAccountService {

    fun account(accountNameOwner: String): Optional<Account>
    //fun findByActiveStatusAndAccountTypeAndTotalsIsGreaterThanOrderByAccountNameOwner(): List<Account>
    fun accounts(): List<Account>
    fun findAccountsThatRequirePayment(): List<Account>
    fun sumOfAllTransactionsByTransactionState(transactionState: TransactionState): BigDecimal
    fun insertAccount(account: Account): Account
    fun deleteAccount(accountNameOwner: String): Boolean
    fun updateTotalsForAllAccounts(): Boolean
    fun updateAccount(account: Account): Account
    fun renameAccountNameOwner(oldAccountNameOwner: String, newAccountNameOwner: String): Account
}