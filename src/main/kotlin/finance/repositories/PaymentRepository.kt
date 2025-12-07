package finance.repositories

import finance.domain.Payment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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

    // Find payments that reference a specific transaction GUID (either as source or destination)
    fun findByGuidSourceOrGuidDestination(
        guidSource: String,
        guidDestination: String,
    ): List<Payment>

    // Paginated query for active payments (note: Payment uses activeStatus field)
    fun findByActiveStatusOrderByTransactionDateDesc(
        activeStatus: Boolean = true,
        pageable: Pageable,
    ): Page<Payment>

//    @Transactional
//    fun deleteByPaymentId(paymentId: Long)
}
