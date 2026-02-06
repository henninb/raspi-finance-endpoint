package finance.repositories

import finance.domain.ReceiptImage
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface ReceiptImageRepository : JpaRepository<ReceiptImage, Long> {
    fun findByTransactionId(transactionId: Long): Optional<ReceiptImage>

    // --- Owner-scoped methods for multi-tenancy (Phase 4) ---

    fun findByOwnerAndTransactionId(
        owner: String,
        transactionId: Long,
    ): Optional<ReceiptImage>
}
