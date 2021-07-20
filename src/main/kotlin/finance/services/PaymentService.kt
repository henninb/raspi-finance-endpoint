package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.*
import finance.repositories.PaymentRepository
import io.micrometer.core.annotation.Timed
import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.*
import javax.validation.ConstraintViolation
import javax.validation.ValidationException
import javax.validation.Validator

@Service
open class PaymentService(
    private var paymentRepository: PaymentRepository,
    private var transactionService: TransactionService,
    private var accountService: AccountService,
    private var parameterService: ParameterService,
    private val validator: Validator,
    private var meterService: MeterService
) : IPaymentService, BaseService() {

    @Timed
    override fun findAllPayments(): List<Payment> {
        return paymentRepository.findAll().sortedByDescending { payment -> payment.transactionDate }
    }

    //TODO: make this method transactional - what happens if one inserts fails?
    @Timed
    override fun insertPayment(payment: Payment): Payment {
        val transactionCredit = Transaction()
        val transactionDebit = Transaction()

        val constraintViolations: Set<ConstraintViolation<Payment>> = validator.validate(payment)
        handleConstraintViolations(constraintViolations, meterService)
        val optionalAccount = accountService.findByAccountNameOwner(payment.accountNameOwner)
        if (!optionalAccount.isPresent) {
            logger.error("Account not found ${payment.accountNameOwner}")
            meterService.incrementExceptionThrownCounter("RuntimeException")
            throw RuntimeException("Account not found ${payment.accountNameOwner}")
        } else {
            if (optionalAccount.get().accountType == AccountType.Debit) {
                logger.error("Account cannot make a payment to a debit account: ${payment.accountNameOwner}")
                meterService.incrementExceptionThrownCounter("RuntimeException")
                throw RuntimeException("Account cannot make a payment to a debit account: ${payment.accountNameOwner}")
            }
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
            return paymentRepository.saveAndFlush(payment)
        }
        throw RuntimeException("failed to read the parameter 'payment_account'.")
    }

    //TODO: 10/24/2020 - not sure if Throws annotation helps here?
    //TODO: 10/24/2020 - Should an exception throw a 500 at the endpoint?
    @Throws
    @Timed
    override fun populateDebitTransaction(
        transactionDebit: Transaction,
        payment: Payment,
        paymentAccountNameOwner: String
    ) {
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
        transactionDebit.reoccurringType = ReoccurringType.Onetime
        transactionDebit.accountType = AccountType.Debit
        transactionDebit.accountNameOwner = paymentAccountNameOwner
        transactionDebit.dateUpdated = Timestamp(Calendar.getInstance().time.time)
        transactionDebit.dateAdded = Timestamp(Calendar.getInstance().time.time)
    }

    @Timed
    override fun populateCreditTransaction(
        transactionCredit: Transaction,
        payment: Payment,
        paymentAccountNameOwner: String
    ) {
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
        transactionCredit.reoccurringType = ReoccurringType.Onetime
        transactionCredit.accountType = AccountType.Credit
        transactionCredit.accountNameOwner = payment.accountNameOwner
        transactionCredit.dateUpdated = Timestamp(Calendar.getInstance().time.time)
        transactionCredit.dateAdded = Timestamp(Calendar.getInstance().time.time)
    }

    @Timed
    override fun deleteByPaymentId(paymentId: Long) : Boolean {
        logger.info("service - deleteByPaymentId = $paymentId")
        paymentRepository.deleteByPaymentId(paymentId)
        return true
    }

    @Timed
    override fun findByPaymentId(paymentId: Long): Optional<Payment> {
        logger.info("service - findByPaymentId = $paymentId")
        val paymentOptional: Optional<Payment> = paymentRepository.findByPaymentId(paymentId)
        if (paymentOptional.isPresent) {
            return paymentOptional
        }
        return Optional.empty()
    }
}
