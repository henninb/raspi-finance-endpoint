package finance.repositories

import finance.domain.TransactionState
import finance.domain.ValidationAmount
import org.springframework.data.jpa.repository.JpaRepository
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

    fun findByOwnerAndValidationIdAndActiveStatusTrue(
        owner: String,
        validationId: Long,
    ): Optional<ValidationAmount>
}
