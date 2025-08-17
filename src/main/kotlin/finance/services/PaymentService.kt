package finance.services

import finance.domain.*
import finance.repositories.PaymentRepository
import io.micrometer.core.annotation.Timed
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.*
import jakarta.validation.ConstraintViolation
import jakarta.validation.ValidationException

@Service
open class PaymentService(
    private var paymentRepository: PaymentRepository,
    private var transactionService: TransactionService,
    private var accountService: AccountService,
    private var parameterService: ParameterService
) : IPaymentService, BaseService() {

    @Timed
    override fun findAllPayments(): List<Payment> {
        logger.info("Fetching all payments")
        val payments = paymentRepository.findAll().sortedByDescending { payment -> payment.transactionDate }
        logger.info("Found ${payments.size} payments")
        return payments
    }

    @Timed
    override fun updatePayment(paymentId: Long, patch: Payment): Payment {
        logger.info("Updating payment with ID: $paymentId")

        val existing = paymentRepository.findByPaymentId(paymentId)
            .orElseThrow { ValidationException("Payment not found: $paymentId") }

        // Determine new values; require at least one meaningful change
        val newDate = if (patch.transactionDate.time != 0L) patch.transactionDate else existing.transactionDate
        val newAmount = if (patch.amount != BigDecimal(0.00)) patch.amount else existing.amount

        // Load linked transactions by GUIDs and update to keep in sync
        val sourceGuid = existing.guidSource
        val destGuid = existing.guidDestination

        if (sourceGuid.isNullOrBlank() || destGuid.isNullOrBlank()) {
            logger.error("Payment $paymentId missing transaction GUIDs: source=$sourceGuid, dest=$destGuid")
            throw ValidationException("Payment $paymentId is missing linked transaction GUIDs.")
        }

        val debitTx = transactionService.findTransactionByGuid(sourceGuid)
            .orElseThrow { ValidationException("Source transaction not found for payment $paymentId: $sourceGuid") }
        val creditTx = transactionService.findTransactionByGuid(destGuid)
            .orElseThrow { ValidationException("Destination transaction not found for payment $paymentId: $destGuid") }

        // Update dates
        debitTx.transactionDate = newDate
        creditTx.transactionDate = newDate

        // Apply same amount logic used on insert
        debitTx.amount = if (newAmount > BigDecimal(0.0)) newAmount * BigDecimal(-1.0) else newAmount
        creditTx.amount = if (newAmount > BigDecimal(0.0)) newAmount * BigDecimal(-1.0) else newAmount

        // Persist transactions first to ensure FK consistency
        transactionService.updateTransaction(debitTx)
        transactionService.updateTransaction(creditTx)

        // Update and persist payment
        existing.transactionDate = newDate
        existing.amount = newAmount
        existing.dateUpdated = Timestamp(System.currentTimeMillis())

        val saved = paymentRepository.saveAndFlush(existing)
        logger.info("Successfully updated payment with ID: ${saved.paymentId}")
        return saved
    }


    @Timed
    override fun insertPaymentNew(payment: Payment): Payment {
        logger.info("Inserting new payment for account: ${payment.accountNameOwner}")
        val transactionCredit = Transaction()
        val transactionDebit = Transaction()

        val constraintViolations: Set<ConstraintViolation<Payment>> = validator.validate(payment)
        handleConstraintViolations(constraintViolations, meterService)
        val optionalAccount = accountService.account(payment.accountNameOwner)
        if (!optionalAccount.isPresent) {
            logger.error("Account not found ${payment.accountNameOwner}")
            meterService.incrementExceptionThrownCounter("ValidationException")
            throw ValidationException("Account not found ${payment.accountNameOwner}")
        } else {
            if (optionalAccount.get().accountType == AccountType.Debit) {
                logger.error("Account cannot make a payment to a debit account: ${payment.accountNameOwner}")
                meterService.incrementExceptionThrownCounter("ValidationException")
                throw ValidationException("Account cannot make a payment to a debit account: ${payment.accountNameOwner}")
            }
        }

        val paymentAccountNameOwner = payment.sourceAccount
        populateCreditTransaction(transactionCredit, payment, paymentAccountNameOwner)
        populateDebitTransaction(transactionDebit, payment, paymentAccountNameOwner)

        logger.info("Creating debit and credit transactions for payment")
        transactionService.insertTransaction(transactionCredit)
        transactionService.insertTransaction(transactionDebit)
        payment.guidDestination = transactionCredit.guid
        payment.guidSource = transactionDebit.guid
        val timestamp = Timestamp(System.currentTimeMillis())
        payment.dateUpdated = timestamp
        payment.dateAdded = timestamp
        val savedPayment = paymentRepository.saveAndFlush(payment)
        logger.info("Successfully created payment with ID: ${savedPayment.paymentId}")
        return savedPayment
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
        val timestamp = Timestamp(System.currentTimeMillis())
        transactionDebit.dateUpdated = timestamp
        transactionDebit.dateAdded = timestamp
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
        val timestamp = Timestamp(System.currentTimeMillis())
        transactionCredit.dateUpdated = timestamp
        transactionCredit.dateAdded = timestamp
    }

    @Timed
    override fun deleteByPaymentId(paymentId: Long): Boolean {
        logger.info("Deleting payment with ID: $paymentId")
        val paymentOptional = paymentRepository.findByPaymentId(paymentId)
        if (paymentOptional.isPresent) {
            paymentRepository.delete(paymentOptional.get())
            logger.info("Successfully deleted payment with ID: $paymentId")
            return true
        }
        logger.warn("Payment not found with ID: $paymentId")
        return false
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

    @Deprecated("Use insertPaymentNew instead")
    @Timed
    fun insertPayment(payment: Payment): Payment {
        val transactionCredit = Transaction()
        val transactionDebit = Transaction()

        val constraintViolations: Set<ConstraintViolation<Payment>> = validator.validate(payment)
        handleConstraintViolations(constraintViolations, meterService)
        val optionalAccount = accountService.account(payment.accountNameOwner)
        if (!optionalAccount.isPresent) {
            logger.error("Account not found ${payment.accountNameOwner}")
            meterService.incrementExceptionThrownCounter("ValidationException")
            throw ValidationException("Account not found ${payment.accountNameOwner}")
        } else {
            if (optionalAccount.get().accountType == AccountType.Debit) {
                logger.error("Account cannot make a payment to a debit account: ${payment.accountNameOwner}")
                meterService.incrementExceptionThrownCounter("ValidationException")
                throw ValidationException("Account cannot make a payment to a debit account: ${payment.accountNameOwner}")
            }
        }

        val optionalParameter = parameterService.findByParameterName("payment_account")
        if (optionalParameter.isPresent) {
            val paymentAccountNameOwner = optionalParameter.get().parameterValue
            populateCreditTransaction(transactionCredit, payment, paymentAccountNameOwner)
            populateDebitTransaction(transactionDebit, payment, paymentAccountNameOwner)

            transactionService.insertTransaction(transactionCredit)
            transactionService.insertTransaction(transactionDebit)
            payment.guidDestination = transactionCredit.guid
            payment.guidSource = transactionDebit.guid
            val timestamp = Timestamp(System.currentTimeMillis())
            payment.dateUpdated = timestamp
            payment.dateAdded = timestamp
            return paymentRepository.saveAndFlush(payment)
        } else {
            logger.error("Parameter not found: payment_account")
            meterService.incrementExceptionThrownCounter("ValidationException")
            throw ValidationException("Parameter not found: payment_account")
        }
    }
}
