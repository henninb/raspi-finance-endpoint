package finance.repositories

import finance.domain.Account
import finance.domain.AccountType
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.util.*
//import javax.transaction.Transactional

interface AccountRepository : JpaRepository<Account, Long> {
    fun findByAccountNameOwner(accountNameOwner: String): Optional<Account>
    fun findByAccountId(accountId: Long): Optional<Account>
    fun findByActiveStatusOrderByAccountNameOwner(activeStatus: Boolean = true): List<Account>
    //TODO: 5/22/2022 - bh need to change the code to do this
    //fun findByActiveStatusIsTrueOrderByAccountNameOwner() : List<Account>

    fun findByActiveStatusAndAccountTypeOrderByAccountNameOwner(
        activeStatus: Boolean = true,
        accountType: AccountType = AccountType.Credit
    ): List<Account>

    @Modifying
    @Transactional
    @Query(
        "UPDATE t_account SET cleared = x.cleared, outstanding = x.outstanding, future = x.future, date_updated = now() FROM " +
                "(SELECT account_name_owner, SUM(case when transaction_state='cleared' THEN amount ELSE 0 END) AS cleared, sum(case when transaction_state='outstanding' THEN amount ELSE 0 END) AS outstanding, sum(CASE WHEN transaction_state='future' THEN amount ELSE 0 END) AS future FROM t_transaction WHERE active_status = true group by account_name_owner) " +
                "AS x WHERE t_account.account_name_owner = x.account_name_owner", nativeQuery = true
    )
    fun updateTotalsForAllAccounts()

    @Query(
        value = "SELECT COALESCE(A.debits, 0.0) - COALESCE(B.credits, 0.0) FROM ( SELECT SUM(amount) AS debits FROM t_transaction WHERE account_type = 'debit' AND transaction_state = :transactionState AND active_status = true) A,( SELECT SUM(amount) AS credits FROM t_transaction WHERE account_type = 'credit' and transaction_state = :transactionState AND active_status = true) B",
        nativeQuery = true
    )
    fun sumOfAllTransactionsByTransactionState(@Param("transactionState") transactionState: String): BigDecimal

//    @Query(
//        value = """
//        SELECT a.* FROM t_account a
//        WHERE a.active_status = true
//        AND a.account_type = :accountType
//        AND (
//            a.outstanding > 0 OR a.future > 0 OR a.cleared > 0
//        )
//        AND EXISTS (
//            SELECT 1 FROM t_transaction t
//            WHERE t.account_name_owner = a.account_name_owner
//            AND t.active_status = true
//            AND t.transaction_state NOT IN ('cleared')
//            AND t.transaction_date < NOW() + INTERVAL '30 days'
//        )
//        ORDER BY a.account_name_owner
//    """,
//        nativeQuery = true
//    )
//    fun findAccountsThatRequirePayment(
//        @Param("accountType") accountType: String = "credit"
//    ): List<Account>


    fun findByActiveStatusAndAccountTypeAndOutstandingGreaterThanOrFutureGreaterThanOrClearedGreaterThanOrderByAccountNameOwner(
        activeStatus: Boolean = true,
        accountType: AccountType = AccountType.Credit,
        outstanding: BigDecimal = BigDecimal.ZERO,
        future: BigDecimal = BigDecimal.ZERO,
        cleared: BigDecimal = BigDecimal.ZERO
    ): List<Account>
}
