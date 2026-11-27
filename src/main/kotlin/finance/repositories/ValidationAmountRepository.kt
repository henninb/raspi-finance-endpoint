package finance.repositories

import finance.domain.TransactionState
import finance.domain.ValidationAmount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

interface ValidationAmountRepository : JpaRepository<ValidationAmount, Long> {
    // Legacy methods
    fun findByTransactionStateAndAccountId(
        transactionState: TransactionState,
        accountId: Long,
    ): List<ValidationAmount>

    fun findByAccountId(accountId: Long): List<ValidationAmount>

    // Standardized methods
    fun findByActiveStatusTrueOrderByValidationDateDesc(): List<ValidationAmount>

    fun findByValidationIdAndActiveStatusTrue(validationId: Long): Optional<ValidationAmount>

    // --- Owner-scoped methods for multi-tenancy (Phase 4) ---

    fun findByOwnerAndTransactionStateAndAccountId(
        owner: String,
        transactionState: TransactionState,
        accountId: Long,
    ): List<ValidationAmount>

    fun findByOwnerAndAccountId(
        owner: String,
        accountId: Long,
    ): List<ValidationAmount>

    fun findByOwnerAndActiveStatusTrueOrderByValidationDateDesc(
        owner: String,
    ): List<ValidationAmount>

    // Filtered in the database, so a page asking about one account does not load
    // every validation amount of the owner (4,478 rows in production) and filter in memory.
    fun findByOwnerAndAccountIdAndActiveStatusTrueOrderByValidationDateDesc(
        owner: String,
        accountId: Long,
    ): List<ValidationAmount>

    fun findByOwnerAndAccountIdAndTransactionStateAndActiveStatusTrueOrderByValidationDateDesc(
        owner: String,
        accountId: Long,
        transactionState: TransactionState,
    ): List<ValidationAmount>

    fun findByOwnerAndTransactionStateAndActiveStatusTrueOrderByValidationDateDesc(
        owner: String,
        transactionState: TransactionState,
    ): List<ValidationAmount>

    fun findByOwnerAndValidationIdAndActiveStatusTrue(
        owner: String,
        validationId: Long,
    ): Optional<ValidationAmount>

    @Modifying
    @Transactional
    @Query("DELETE FROM ValidationAmount v WHERE v.owner = :owner AND v.accountId = :accountId")
    fun deleteByOwnerAndAccountId(
        @Param("owner") owner: String,
        @Param("accountId") accountId: Long,
    ): Int
}
