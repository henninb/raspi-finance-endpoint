package finance.services

import finance.domain.Payment
import finance.repositories.PaymentRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class PaymentService (private var paymentRepository: PaymentRepository) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun findAllPayments(): List<Payment> {
        return paymentRepository.findAll()
    }

    fun insertPayment(payment: Payment): Boolean {
        paymentRepository.save(payment)
        return true
    }

    fun deleteByPaymentId(paymentId: Long) {
        logger.info("service - deleteByPaymentId = $paymentId")
        paymentRepository.deleteByPaymentId(paymentId)
    }

    fun findByPaymentId(paymentId: Long): Optional<Payment> {
        logger.info("service - findByPaymentId = $paymentId")
        val paymentOptional: Optional<Payment> = paymentRepository.findByPaymentId(paymentId)
        if (paymentOptional.isPresent) {
            return paymentOptional
        }
        return Optional.empty()
    }
}