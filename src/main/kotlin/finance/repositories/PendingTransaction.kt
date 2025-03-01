package finance.repositories

import finance.domain.PendingTransaction
import org.springframework.data.jpa.repository.JpaRepository

interface PendingTransactionRepository : JpaRepository<PendingTransaction, Long> {
}