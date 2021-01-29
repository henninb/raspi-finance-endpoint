package finance.services

import finance.domain.Payment
import finance.domain.Transaction
import java.util.*

interface IPaymentService {
    fun findAllPayments(): List<Payment>

    fun insertPayment(payment: Payment): Boolean

    fun populateDebitTransaction(
        transactionDebit: Transaction,
        payment: Payment,
        paymentAccountNameOwner: String
    )

    fun populateCreditTransaction(
        transactionCredit: Transaction,
        payment: Payment,
        paymentAccountNameOwner: String
    )

    fun deleteByPaymentId(paymentId: Long)
    fun findByPaymentId(paymentId: Long): Optional<Payment>
}