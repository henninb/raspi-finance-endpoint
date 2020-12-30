package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.AccountType
import finance.domain.Payment
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.repositories.PaymentRepository
import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.*
import javax.validation.ConstraintViolation
import javax.validation.ValidationException
import javax.validation.Validator

@Service
class PaymentService(private var paymentRepository: PaymentRepository,
                     private var transactionService: TransactionService,
                     private var parameterService: ParameterService,
                     private val validator: Validator,
                     private var meterService: MeterService) {

    fun findAllPayments(): List<Payment> {
        return paymentRepository.findAll().sortedByDescending { payment -> payment.transactionDate }
    }

    //TODO: make this method transactional - what happens if one inserts fails?
    //@Timed
    fun insertPayment(payment: Payment): Boolean {
        val transactionCredit = Transaction()
        val transactionDebit = Transaction()

        val constraintViolations: Set<ConstraintViolation<Payment>> = validator.validate(payment)
        if (constraintViolations.isNotEmpty()) {
            constraintViolations.forEach { constraintViolation -> logger.error(constraintViolation.message) }
            logger.error("Cannot insert payment as there is a constraint violation on the data.")
            throw ValidationException("Cannot insert payment as there is a constraint violation on the data.")
        }

        val optionalParameter = parameterService.findByParameter("payment_account")
        if (optionalParameter.isPresent) {
            val paymentAccountNameOwner = optionalParameter.get().parameterValue
            populateCreditTransaction(transactionCredit, payment, paymentAccountNameOwner)
            populateDebitTransaction(transactionDebit, payment, paymentAccountNameOwner)

            transactionService.insertTransaction(transactionCredit)
            transactionService.insertTransaction(transactionDebit)
            payment.guidDestination = transactionCredit.guid
            payment.guidSource = transactionDebit.guid
            payment.dateUpdated = Timestamp(Calendar.getInstance().time.time)
            payment.dateAdded = Timestamp(Calendar.getInstance().time.time)
            paymentRepository.saveAndFlush(payment)
            return true
        }
        throw RuntimeException("failed to read the parameter 'payment_account'.")
    }

    //TODO: 10/24/2020 - not sure if Throws annotation helps here?
    //TODO: 10/24/2020 - Should an exception throw a 500 at the endpoint?
    @Throws
    private fun populateDebitTransaction(transactionDebit: Transaction, payment: Payment, paymentAccountNameOwner: String) {
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
        transactionDebit.transactionState = TransactionState.Outstanding
        transactionDebit.accountType = AccountType.Debit
        transactionDebit.reoccurring = false
        transactionDebit.accountNameOwner = paymentAccountNameOwner
        transactionDebit.dateUpdated = Timestamp(Calendar.getInstance().time.time)
        transactionDebit.dateAdded = Timestamp(Calendar.getInstance().time.time)
    }

    private fun populateCreditTransaction(transactionCredit: Transaction, payment: Payment, paymentAccountNameOwner: String) {
        transactionCredit.guid = UUID.randomUUID().toString()
        transactionCredit.transactionDate = payment.transactionDate
        transactionCredit.description = "payment"
        transactionCredit.category = "bill_pay"
        transactionCredit.notes = "from $paymentAccountNameOwner"
        when {
            payment.amount > BigDecimal(0.0) -> {
                transactionCredit.amount = payment.amount * BigDecimal(-1.0)
            }
            else -> {
                transactionCredit.amount = payment.amount
            }
        }

        transactionCredit.transactionState = TransactionState.Outstanding
        transactionCredit.accountType = AccountType.Credit
        transactionCredit.reoccurring = false
        transactionCredit.accountNameOwner = payment.accountNameOwner
        transactionCredit.dateUpdated = Timestamp(Calendar.getInstance().time.time)
        transactionCredit.dateAdded = Timestamp(Calendar.getInstance().time.time)
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

    companion object {
        private val mapper = ObjectMapper()
        private val logger = LogManager.getLogger()
    }
}