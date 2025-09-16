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

        // Determine what values are being updated (flexible approach for any field changes)
        val newDate = if (patch.transactionDate.time != 0L) patch.transactionDate else existing.transactionDate
        val newAmount = if (patch.amount != BigDecimal(0.00)) patch.amount else existing.amount
        val newSourceAccount = if (patch.sourceAccount.isNotBlank()) patch.sourceAccount else existing.sourceAccount
        val newDestinationAccount = if (patch.destinationAccount.isNotBlank()) patch.destinationAccount else existing.destinationAccount
        val newActiveStatus = patch.activeStatus ?: existing.activeStatus

        // Check if any meaningful changes are being made
        val dateChanged = newDate != existing.transactionDate
        val amountChanged = newAmount != existing.amount
        val destinationChanged = newDestinationAccount != existing.destinationAccount
        val sourceAccountChanged = newSourceAccount != existing.sourceAccount
        val destinationAccountChanged = newDestinationAccount != existing.destinationAccount
        val statusChanged = newActiveStatus != existing.activeStatus

        if (!dateChanged && !amountChanged && !sourceAccountChanged && !destinationAccountChanged && !statusChanged) {
            logger.info("No changes detected for payment $paymentId, returning existing record")
            return existing
        }

        // Check for duplicate payments BEFORE making changes (excluding current payment)
        // Only check for duplicates if the unique constraint fields (destinationAccount, date, amount) are changing
        if (dateChanged || amountChanged || destinationChanged) {
            val duplicatePayment = paymentRepository.findByDestinationAccountAndTransactionDateAndAmountAndPaymentIdNot(
                newDestinationAccount, newDate, newAmount, paymentId
            )

            if (duplicatePayment.isPresent) {
                val existingId = duplicatePayment.get().paymentId
                val msg = "Duplicate payment conflict: destination_account='${newDestinationAccount}', transaction_date='${newDate}', amount=${newAmount} already exists as payment_id=${existingId}. Unique key = (destination_account, transaction_date, amount)."
                logger.error(msg + " (excluding current payment $paymentId)")
                meterService.incrementExceptionThrownCounter("DuplicatePaymentException")
                throw org.springframework.dao.DataIntegrityViolationException(msg)
            }
        }

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

        // Update linked transactions if date or amount changed
        if (dateChanged) {
            logger.info("Updating transaction dates for payment $paymentId: $newDate")
            debitTx.transactionDate = newDate
            creditTx.transactionDate = newDate
        }

        if (amountChanged) {
            logger.info("Updating transaction amounts for payment $paymentId: $newAmount")
            // Apply same amount logic used on insert
            debitTx.amount = if (newAmount > BigDecimal(0.0)) newAmount * BigDecimal(-1.0) else newAmount
            creditTx.amount = if (newAmount > BigDecimal(0.0)) newAmount * BigDecimal(-1.0) else newAmount
        }

        // Persist transactions first to ensure FK consistency (only if they changed)
        if (dateChanged || amountChanged) {
            transactionService.updateTransaction(debitTx)
            transactionService.updateTransaction(creditTx)
        }

        // Update and persist payment with all changed fields
        existing.transactionDate = newDate
        existing.amount = newAmount
        existing.sourceAccount = newSourceAccount
        existing.destinationAccount = newDestinationAccount
        existing.activeStatus = newActiveStatus
        existing.dateUpdated = Timestamp(System.currentTimeMillis())

        val saved = paymentRepository.saveAndFlush(existing)
        logger.info("Successfully updated payment with ID: ${saved.paymentId} - Changes: dateChanged=$dateChanged, amountChanged=$amountChanged, sourceAccountChanged=$sourceAccountChanged, destinationAccountChanged=$destinationAccountChanged, statusChanged=$statusChanged")
        return saved
    }


    @Timed
    override fun insertPayment(payment: Payment): Payment {
        logger.info("Inserting new payment to destination account: ${payment.destinationAccount}")
        val transactionCredit = Transaction()
        val transactionDebit = Transaction()

        val constraintViolations: Set<ConstraintViolation<Payment>> = validator.validate(payment)
        handleConstraintViolations(constraintViolations, meterService)

        // Process destination account - create if missing (like TransactionService does)
        processPaymentAccount(payment.destinationAccount)

        // Process source account - create if missing
        processPaymentAccount(payment.sourceAccount)

        // Validate destination account type after ensuring it exists
        val destinationAccount = accountService.account(payment.destinationAccount).get()
        if (destinationAccount.accountType == AccountType.Debit) {
            logger.error("Account cannot make a payment to a debit account: ${payment.destinationAccount}")
            meterService.incrementExceptionThrownCounter("ValidationException")
            throw ValidationException("Account cannot make a payment to a debit account: ${payment.destinationAccount}")
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

    /**
     * Process payment account - create if missing (similar to TransactionService.processAccount)
     */
    private fun processPaymentAccount(accountNameOwner: String) {
        logger.info("Finding account: $accountNameOwner")
        val accountOptional = accountService.account(accountNameOwner)
        if (accountOptional.isPresent) {
            logger.info("Using existing account: $accountNameOwner")
        } else {
            logger.info("Creating new account: $accountNameOwner")
            val account = createDefaultAccount(accountNameOwner, AccountType.Credit)
            val savedAccount = accountService.insertAccount(account)
            logger.info("Created new account: $accountNameOwner with ID: ${savedAccount.accountId}")
        }
    }

    /**
     * Create a default account (similar to TransactionService.createDefaultAccount)
     */
    private fun createDefaultAccount(accountNameOwner: String, accountType: AccountType): Account {
        val account = Account()
        account.accountNameOwner = accountNameOwner
        account.moniker = "0000"
        account.accountType = accountType
        account.activeStatus = true
        return account
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
        transactionDebit.notes = "to ${payment.destinationAccount}"
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
        transactionCredit.accountNameOwner = payment.destinationAccount
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

}
