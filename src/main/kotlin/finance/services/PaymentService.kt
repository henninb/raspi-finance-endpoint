package finance.services

import finance.domain.Payment
import finance.repositories.PaymentRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PaymentService (private var paymentRepository: PaymentRepository) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun findAllPayments(): List<Payment> {
        return paymentRepository.findAll()
    }
}