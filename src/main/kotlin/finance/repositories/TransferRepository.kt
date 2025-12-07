package finance.repositories

import finance.domain.Transfer
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface TransferRepository : JpaRepository<Transfer, Long> {
    fun findByTransferId(paymentId: Long): Optional<Transfer>

    // Paginated query for active transfers
    fun findByActiveStatusOrderByTransactionDateDesc(
        activeStatus: Boolean = true,
        pageable: Pageable,
    ): Page<Transfer>

//    @Transactional
//    fun deleteByPaymentId(paymentId: Long)
}
