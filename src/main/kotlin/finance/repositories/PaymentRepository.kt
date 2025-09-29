package finance.repositories

import finance.domain.Payment
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PaymentRepository : JpaRepository<Payment, Long> {
    fun findByPaymentId(paymentId: Long): Optional<Payment>

    // Check for duplicate payments excluding the current payment being updated
    fun findByDestinationAccountAndTransactionDateAndAmountAndPaymentIdNot(
        destinationAccount: String,
        transactionDate: java.sql.Date,
        amount: java.math.BigDecimal,
        paymentId: Long,
    ): Optional<Payment>

//    @Transactional
//    fun deleteByPaymentId(paymentId: Long)
}
