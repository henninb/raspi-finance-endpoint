package finance.repositories

import finance.domain.PendingTransaction
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PendingTransactionRepository : JpaRepository<PendingTransaction, Long> {
    fun findByPendingTransactionIdOrderByTransactionDateDesc(pendingTransactionId: Long): Optional<PendingTransaction>
}
