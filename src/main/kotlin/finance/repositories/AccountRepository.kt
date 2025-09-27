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
    fun findByActiveStatus(activeStatus: Boolean): List<Account>
    fun findByAccountType(accountType: AccountType): List<Account>
    fun findByActiveStatusAndAccountType(activeStatus: Boolean, accountType: AccountType): List<Account>

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


    @Query(
        """SELECT COALESCE(SUM(CASE WHEN t.accountType = 'debit' THEN t.amount ELSE 0 END), 0) -
              COALESCE(SUM(CASE WHEN t.accountType = 'credit' THEN t.amount ELSE 0 END), 0)
       FROM Transaction t
       WHERE t.transactionState = :transactionState
       AND t.activeStatus = true"""
    )
    fun sumOfAllTransactionsByTransactionStateJpql(@Param("transactionState") transactionState: String): BigDecimal

    @Query(
        """SELECT a FROM Account a
       WHERE a.activeStatus = :activeStatus
       AND a.accountType = :accountType
       AND (a.outstanding > 0 OR a.future > 0 OR a.cleared > 0)
       ORDER BY a.accountNameOwner"""
    )
    fun findAccountsThatRequirePayment(
        @Param("activeStatus") activeStatus: Boolean = true,
        @Param("accountType") accountType: AccountType = AccountType.Credit
    ): List<Account>

    // Update validation_date for a single account from the newest t_validation_amount row
    @Modifying
    @Transactional
    @Query(
        value = """
            UPDATE t_account a
            SET validation_date = sub.max_validation_date,
                date_updated = now()
            FROM (
                SELECT va.account_id, MAX(va.validation_date) AS max_validation_date
                FROM t_validation_amount va
                WHERE va.active_status = TRUE
                GROUP BY va.account_id
            ) sub
            WHERE a.account_id = sub.account_id
            AND a.account_id = :accountId
        """,
        nativeQuery = true
    )
    fun updateValidationDateForAccount(@Param("accountId") accountId: Long): Int

    // Bulk refresh of validation_date for all accounts from newest ValidationAmount
    @Modifying
    @Transactional
    @Query(
        value = """
            UPDATE t_account a
            SET validation_date = sub.max_validation_date,
                date_updated = now()
            FROM (
                SELECT va.account_id, MAX(va.validation_date) AS max_validation_date
                FROM t_validation_amount va
                WHERE va.active_status = TRUE
                GROUP BY va.account_id
            ) sub
            WHERE a.account_id = sub.account_id
        """,
        nativeQuery = true
    )
    fun updateValidationDateForAllAccounts()

}
