package finance.repositories

import finance.domain.Payment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface PaymentRepository : JpaRepository<Payment, Long> {
    fun findByPaymentId(paymentId: Long): Optional<Payment>

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM t_payment WHERE payment_id = ?1", nativeQuery = true)
    fun deleteByPaymentId(paymentId: Long)
}