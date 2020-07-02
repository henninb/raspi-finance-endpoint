package finance.repositories

import finance.domain.Account
import finance.domain.AccountType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.*
import javax.transaction.Transactional

interface AccountRepository : JpaRepository<Account, Long> {
    fun findByAccountNameOwner(accountNameOwner: String): Optional<Account>
    fun findByActiveStatusOrderByAccountNameOwner(activeStatus: Boolean): List<Account>
    //fun findByAccountType(accountType: AccountType): List<Account>

    @Modifying
    @Transactional
    @Query(value = "DELETE from t_account WHERE account_name_owner = ?1", nativeQuery = true)
    fun deleteByAccountNameOwner(accountNameOwner: String)

    @Modifying
    @Transactional
    @Query(value = "UPDATE t_account SET totals = x.totals FROM (SELECT account_name_owner, SUM(amount) AS totals FROM t_transaction GROUP BY account_name_owner) x WHERE t_account.account_name_owner = x.account_name_owner", nativeQuery = true)
    fun updateAccountClearedTotals()

    @Modifying
    @Transactional
    @Query(value = "UPDATE t_account SET totals_balanced = x.totals_balanced FROM (SELECT account_name_owner, SUM(amount) AS totals_balanced FROM t_transaction WHERE cleared = 1 GROUP BY account_name_owner) x WHERE t_account.account_name_owner = x.account_name_owner", nativeQuery = true)
    fun updateAccountGrandTotals()

    @Query(value = "SELECT (A.debits - B.credits) FROM ( SELECT SUM(amount) AS debits FROM t_transaction WHERE account_type = 'debit' ) A,( SELECT SUM(amount) AS credits FROM t_transaction WHERE account_type = 'credit' ) B", nativeQuery = true)
    fun selectTotals(): Double

    @Query(value = "SELECT (A.debits - B.credits) FROM ( SELECT SUM(amount) AS debits FROM t_transaction WHERE account_type = 'debit' and cleared = 1) A,( SELECT SUM(amount) AS credits FROM t_transaction WHERE account_type = 'credit' and cleared = 1 ) B", nativeQuery = true)
    fun selectTotalsCleared(): Double
}
