package finance.services

import finance.domain.Account
import io.micrometer.core.annotation.Timed
import java.math.BigDecimal
import java.util.*

interface IAccountService {

    fun findByAccountNameOwner(accountNameOwner: String): Optional<Account>

    fun findByActiveStatusAndAccountTypeAndTotalsIsGreaterThanOrderByAccountNameOwner(): List<Account>


    fun findByActiveStatusOrderByAccountNameOwner(): List<Account>


    fun findAccountsThatRequirePayment(): List<String>


    fun computeTheGrandTotalForAllTransactions(): BigDecimal


    fun computeTheGrandTotalForAllClearedTransactions(): BigDecimal


    fun insertAccount(account: Account): Boolean


    fun deleteByAccountNameOwner(accountNameOwner: String): Boolean


    fun updateTheGrandTotalForAllClearedTransactions(): Boolean


    fun updateAccount(account: Account): Boolean
}