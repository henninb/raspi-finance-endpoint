package finance.services

import finance.domain.AccountType
import finance.domain.Payment
import finance.domain.Transaction
import finance.repositories.PaymentRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.*

@Service
class PaymentService (private var paymentRepository: PaymentRepository, private var transactionService: TransactionService) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun findAllPayments(): List<Payment> {
        return paymentRepository.findAll().sortedByDescending { payment -> payment.transactionDate }
    }

    //TODO: make this method transactional
    fun insertPayment(payment: Payment): Boolean {
        val transactionCredit = Transaction()
        val transactionDebit = Transaction()

        populateCreditTransaction(transactionCredit, payment)
        populateDebitTransaction(transactionDebit, payment)

        transactionService.insertTransaction(transactionCredit)
        transactionService.insertTransaction(transactionDebit)
        paymentRepository.save(payment)
        return true
    }

    private fun populateDebitTransaction(transactionDebit: Transaction, payment: Payment) {
        transactionDebit.guid = UUID.randomUUID().toString()
        transactionDebit.transactionDate = payment.transactionDate
        transactionDebit.description = "payment"
        transactionDebit.category = "bill_pay"
        transactionDebit.notes = "to ${payment.accountNameOwner}"
        if (payment.amount > BigDecimal(0.0)) {
            transactionDebit.amount = payment.amount * BigDecimal(-1.0)
        } else {
            transactionDebit.amount = payment.amount
        }
        transactionDebit.cleared = 0;
        transactionDebit.accountType = AccountType.Debit
        transactionDebit.reoccurring = false;
        transactionDebit.accountNameOwner = "bcu-checking_brian"
        transactionDebit.dateUpdated = Timestamp(System.currentTimeMillis())
        transactionDebit.dateAdded = Timestamp(System.currentTimeMillis())
    }

    private fun populateCreditTransaction(transactionCredit: Transaction, payment: Payment) {
        transactionCredit.guid = UUID.randomUUID().toString()
        transactionCredit.transactionDate = payment.transactionDate
        transactionCredit.description = "payment"
        transactionCredit.category = "bill_pay"
        transactionCredit.notes = "from bcu"
        if (payment.amount > BigDecimal(0.0)) {
            transactionCredit.amount = payment.amount * BigDecimal(-1.0)
        } else {
            transactionCredit.amount = payment.amount
        }
        transactionCredit.cleared = 0
        transactionCredit.accountType = AccountType.Credit
        transactionCredit.reoccurring = false
        transactionCredit.accountNameOwner = payment.accountNameOwner;
        transactionCredit.dateUpdated = Timestamp(System.currentTimeMillis())
        transactionCredit.dateAdded = Timestamp(System.currentTimeMillis())
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