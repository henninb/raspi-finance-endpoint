package finance.repositories

import finance.domain.Payment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface PaymentRepository : JpaRepository<Payment, Long> {
    fun findByPaymentId(paymentId: Long): Optional<Payment>

    // Check for duplicate payments excluding the current payment being updated
    fun findByAccountNameOwnerAndTransactionDateAndAmountAndPaymentIdNot(
        accountNameOwner: String,
        transactionDate: java.sql.Date,
        amount: java.math.BigDecimal,
        paymentId: Long
    ): Optional<Payment>

//    @Transactional
//    fun deleteByPaymentId(paymentId: Long)
}