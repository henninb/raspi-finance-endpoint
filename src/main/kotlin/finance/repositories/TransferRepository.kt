package finance.repositories

import finance.domain.Transfer
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface TransferRepository : JpaRepository<Transfer, Long> {
    fun findByTransferId(paymentId: Long): Optional<Transfer>

//    @Transactional
//    fun deleteByPaymentId(paymentId: Long)
}