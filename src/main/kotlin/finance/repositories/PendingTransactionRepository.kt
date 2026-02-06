package finance.repositories

import finance.domain.PendingTransaction
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PendingTransactionRepository : JpaRepository<PendingTransaction, Long> {
    fun findByPendingTransactionIdOrderByTransactionDateDesc(pendingTransactionId: Long): Optional<PendingTransaction>

    // --- Owner-scoped methods for multi-tenancy (Phase 4) ---

    fun findByOwnerAndPendingTransactionIdOrderByTransactionDateDesc(
        owner: String,
        pendingTransactionId: Long,
    ): Optional<PendingTransaction>

    fun findAllByOwner(owner: String): List<PendingTransaction>

    fun deleteAllByOwner(owner: String)
}
