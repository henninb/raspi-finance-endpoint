package finance.repositories

import finance.domain.Account
import finance.domain.AccountType
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.util.Optional
// import javax.transaction.Transactional

interface AccountRepository : JpaRepository<Account, Long> {
    fun findByAccountNameOwner(accountNameOwner: String): Optional<Account>

    fun findByAccountId(accountId: Long): Optional<Account>

    fun findByActiveStatusOrderByAccountNameOwner(activeStatus: Boolean = true): List<Account>

    fun findByActiveStatus(activeStatus: Boolean): List<Account>

    fun findByAccountType(accountType: AccountType): List<Account>

    fun findByActiveStatusAndAccountType(
        activeStatus: Boolean,
        accountType: AccountType,
    ): List<Account>

    // Paginated query for active accounts
    fun findAllByActiveStatusOrderByAccountNameOwner(
        activeStatus: Boolean = true,
        pageable: Pageable,
    ): Page<Account>

    @Modifying
    @Transactional
    @Query(
        "UPDATE t_account SET cleared = x.cleared, outstanding = x.outstanding, future = x.future, date_updated = now() FROM " +
            "(SELECT account_name_owner, SUM(case when transaction_state='cleared' THEN amount ELSE 0 END) AS cleared, sum(case when transaction_state='outstanding' THEN amount ELSE 0 END) AS outstanding, sum(CASE WHEN transaction_state='future' THEN amount ELSE 0 END) AS future FROM t_transaction WHERE active_status = true group by account_name_owner) " +
            "AS x WHERE t_account.account_name_owner = x.account_name_owner",
        nativeQuery = true,
    )
    fun updateTotalsForAllAccounts()

    @Query(
        value = "SELECT COALESCE(A.debits, 0.0) - COALESCE(B.credits, 0.0) FROM ( SELECT SUM(amount) AS debits FROM t_transaction WHERE account_type = 'debit' AND transaction_state = :transactionState AND active_status = true) A,( SELECT SUM(amount) AS credits FROM t_transaction WHERE account_type = 'credit' and transaction_state = :transactionState AND active_status = true) B",
        nativeQuery = true,
    )
    fun sumOfAllTransactionsByTransactionState(
        @Param("transactionState") transactionState: String,
    ): BigDecimal

    @Query(
        """SELECT COALESCE(SUM(CASE WHEN t.accountType = 'debit' THEN t.amount ELSE 0 END), 0) -
              COALESCE(SUM(CASE WHEN t.accountType = 'credit' THEN t.amount ELSE 0 END), 0)
       FROM Transaction t
       WHERE t.transactionState = :transactionState
       AND t.activeStatus = true""",
    )
    fun sumOfAllTransactionsByTransactionStateJpql(
        @Param("transactionState") transactionState: String,
    ): BigDecimal

    @Query(
        """SELECT a FROM Account a
       WHERE a.activeStatus = :activeStatus
       AND a.accountType = :accountType
       AND (a.outstanding > 0 OR a.future > 0 OR a.cleared > 0)
       ORDER BY a.accountNameOwner""",
    )
    fun findAccountsThatRequirePayment(
        @Param("activeStatus") activeStatus: Boolean = true,
        @Param("accountType") accountType: AccountType = AccountType.Credit,
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
        nativeQuery = true,
    )
    fun updateValidationDateForAccount(
        @Param("accountId") accountId: Long,
    ): Int

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
        nativeQuery = true,
    )
    fun updateValidationDateForAllAccounts()

    // --- Owner-scoped methods for multi-tenancy (Phase 4) ---

    fun findByOwnerAndAccountNameOwner(
        owner: String,
        accountNameOwner: String,
    ): Optional<Account>

    fun findByOwnerAndAccountId(
        owner: String,
        accountId: Long,
    ): Optional<Account>

    fun findByOwnerAndActiveStatusOrderByAccountNameOwner(
        owner: String,
        activeStatus: Boolean = true,
    ): List<Account>

    fun findByOwnerAndActiveStatus(
        owner: String,
        activeStatus: Boolean,
    ): List<Account>

    fun findByOwnerAndAccountType(
        owner: String,
        accountType: AccountType,
    ): List<Account>

    fun findByOwnerAndActiveStatusAndAccountType(
        owner: String,
        activeStatus: Boolean,
        accountType: AccountType,
    ): List<Account>

    fun findAllByOwnerAndActiveStatusOrderByAccountNameOwner(
        owner: String,
        activeStatus: Boolean = true,
        pageable: Pageable,
    ): Page<Account>

    @Modifying
    @Transactional
    @Query(
        "UPDATE t_account SET cleared = x.cleared, outstanding = x.outstanding, future = x.future, date_updated = now() FROM " +
            "(SELECT account_name_owner, SUM(case when transaction_state='cleared' THEN amount ELSE 0 END) AS cleared, sum(case when transaction_state='outstanding' THEN amount ELSE 0 END) AS outstanding, sum(CASE WHEN transaction_state='future' THEN amount ELSE 0 END) AS future FROM t_transaction WHERE active_status = true AND owner = :owner group by account_name_owner) " +
            "AS x WHERE t_account.account_name_owner = x.account_name_owner AND t_account.owner = :owner",
        nativeQuery = true,
    )
    fun updateTotalsForAllAccountsByOwner(
        @Param("owner") owner: String,
    )

    @Query(
        value = "SELECT COALESCE(A.debits, 0.0) - COALESCE(B.credits, 0.0) FROM ( SELECT SUM(amount) AS debits FROM t_transaction WHERE account_type = 'debit' AND transaction_state = :transactionState AND active_status = true AND owner = :owner) A,( SELECT SUM(amount) AS credits FROM t_transaction WHERE account_type = 'credit' and transaction_state = :transactionState AND active_status = true AND owner = :owner) B",
        nativeQuery = true,
    )
    fun sumOfAllTransactionsByTransactionStateAndOwner(
        @Param("transactionState") transactionState: String,
        @Param("owner") owner: String,
    ): BigDecimal

    @Query(
        """SELECT COALESCE(SUM(CASE WHEN t.accountType = 'debit' THEN t.amount ELSE 0 END), 0) -
              COALESCE(SUM(CASE WHEN t.accountType = 'credit' THEN t.amount ELSE 0 END), 0)
       FROM Transaction t
       WHERE t.transactionState = :transactionState
       AND t.activeStatus = true
       AND t.owner = :owner""",
    )
    fun sumOfAllTransactionsByTransactionStateAndOwnerJpql(
        @Param("transactionState") transactionState: String,
        @Param("owner") owner: String,
    ): BigDecimal

    @Query(
        """SELECT a FROM Account a
       WHERE a.owner = :owner
       AND a.activeStatus = :activeStatus
       AND a.accountType = :accountType
       AND (a.outstanding > 0 OR a.future > 0 OR a.cleared > 0)
       ORDER BY a.accountNameOwner""",
    )
    fun findAccountsThatRequirePaymentByOwner(
        @Param("owner") owner: String,
        @Param("activeStatus") activeStatus: Boolean = true,
        @Param("accountType") accountType: AccountType = AccountType.Credit,
    ): List<Account>

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
                WHERE va.active_status = TRUE AND va.owner = :owner
                GROUP BY va.account_id
            ) sub
            WHERE a.account_id = sub.account_id
            AND a.account_id = :accountId
            AND a.owner = :owner
        """,
        nativeQuery = true,
    )
    fun updateValidationDateForAccountByOwner(
        @Param("accountId") accountId: Long,
        @Param("owner") owner: String,
    ): Int

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
                WHERE va.active_status = TRUE AND va.owner = :owner
                GROUP BY va.account_id
            ) sub
            WHERE a.account_id = sub.account_id
            AND a.owner = :owner
        """,
        nativeQuery = true,
    )
    fun updateValidationDateForAllAccountsByOwner(
        @Param("owner") owner: String,
    )
}
