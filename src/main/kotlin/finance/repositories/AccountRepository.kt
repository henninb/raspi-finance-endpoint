package finance.repositories

import finance.domain.Account
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.*
import javax.transaction.Transactional

interface AccountRepository : JpaRepository<Account, Long> {
    fun findByAccountNameOwner(accountNameOwner: String): Optional<Account>
    fun findByActiveStatusOrderByAccountNameOwner(activeStatus: Boolean): List<Account>

    //@Modifying
    @Transactional
    //@Query(value = "DELETE from t_account WHERE account_name_owner = ?1", nativeQuery = true)
    fun deleteByAccountNameOwner(accountNameOwner: String)

    @Modifying
    @Transactional
    @Query(value = "UPDATE t_account SET totals = x.totals FROM (SELECT account_name_owner, SUM(amount) AS totals FROM t_transaction WHERE active_status = true GROUP BY account_name_owner) x WHERE t_account.account_name_owner = x.account_name_owner", nativeQuery = true)
    fun updateTheGrandTotalForAllTransactions()

    @Modifying
    @Transactional
    @Query(value = "UPDATE t_account SET totals_balanced = x.totals_balanced FROM (SELECT account_name_owner, SUM(amount) AS totals_balanced FROM t_transaction WHERE transaction_state = 'cleared' AND active_status = true GROUP BY account_name_owner) x WHERE t_account.account_name_owner = x.account_name_owner", nativeQuery = true)
    fun updateTheGrandTotalForAllClearedTransactions()

    @Query(value = "SELECT COALESCE((A.debits - B.credits), 0.0) FROM ( SELECT SUM(amount) AS debits FROM t_transaction WHERE account_type = 'debit' AND active_status = true) A,( SELECT SUM(amount) AS credits FROM t_transaction WHERE account_type = 'credit' AND active_status = true) B", nativeQuery = true)
    fun computeTheGrandTotalForAllTransactions(): Double

    @Query(value = "SELECT COALESCE((A.debits - B.credits), 0.0) FROM ( SELECT SUM(amount) AS debits FROM t_transaction WHERE account_type = 'debit' AND transaction_state = 'cleared' AND active_status = true) A,( SELECT SUM(amount) AS credits FROM t_transaction WHERE account_type = 'credit' and transaction_state = 'cleared' AND active_status = true) B", nativeQuery = true)
    fun computeTheGrandTotalForAllClearedTransactions(): Double

    @Query(value= "SELECT account_name_owner FROM t_transaction WHERE transaction_state = 'cleared' and account_name_owner in (select account_name_owner from t_account where account_type = 'credit' and active_status = true) or (transaction_state = 'outstanding' and account_type = 'credit' and description ='payment') group by account_name_owner having sum(amount) > 0 order by account_name_owner", nativeQuery = true)
    fun findAccountsThatRequirePayment() : List<String>
}
