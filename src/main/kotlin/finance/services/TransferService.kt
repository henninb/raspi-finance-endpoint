package finance.services

import finance.domain.*
import finance.repositories.TransferRepository
import io.micrometer.core.annotation.Timed
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.*
import jakarta.validation.ConstraintViolation

@Service
open class TransferService(
    private var transferRepository: TransferRepository,
    private var transactionService: TransactionService,
    private var accountService: AccountService
) : ITransferService, BaseService() {

    @Timed
    override fun findAllTransfers(): List<Transfer> {
        return transferRepository.findAll().sortedByDescending { transfer -> transfer.transactionDate }
    }

//    @Timed
//    override fun insertTransfer(transfer: Transfer): Transfer {
//        transfer.activeStatus = true
//        transfer.dateUpdated = Timestamp(Calendar.getInstance().time.time)
//        transfer.dateAdded = Timestamp(Calendar.getInstance().time.time)
//
//        return transferRepository.saveAndFlush(transfer)
//    }

    @Timed
    override fun insertTransfer(transfer: Transfer): Transfer {
        val transactionSource = Transaction()
        val transactionDestination = Transaction()

        // Validate transfer
        val constraintViolations: Set<ConstraintViolation<Transfer>> = validator.validate(transfer)
        handleConstraintViolations(constraintViolations, meterService)

        // Validate source account
        val optionalSourceAccount = accountService.account(transfer.sourceAccount)
        if (!optionalSourceAccount.isPresent) {
            logger.error("Source account not found: ${transfer.sourceAccount}")
            meterService.incrementExceptionThrownCounter("RuntimeException")
            throw RuntimeException("Source account not found: ${transfer.sourceAccount}")
        }

        // Validate destination account
        val optionalDestinationAccount = accountService.account(transfer.destinationAccount)
        if (!optionalDestinationAccount.isPresent) {
            logger.error("Destination account not found: ${transfer.destinationAccount}")
            meterService.incrementExceptionThrownCounter("RuntimeException")
            throw RuntimeException("Destination account not found: ${transfer.destinationAccount}")
        }

        // Populate source and destination transactions
        populateSourceTransaction(transactionSource, transfer, transfer.sourceAccount)
        populateDestinationTransaction(transactionDestination, transfer, transfer.destinationAccount)

        // Save transactions and transfer
        transactionService.insertTransaction(transactionSource)
        transactionService.insertTransaction(transactionDestination)

        transfer.guidSource = transactionSource.guid
        transfer.guidDestination = transactionDestination.guid
        transfer.dateUpdated = Timestamp(Calendar.getInstance().time.time)
        transfer.dateAdded = Timestamp(Calendar.getInstance().time.time)

        return transferRepository.saveAndFlush(transfer)
    }

    @Timed
    private fun populateSourceTransaction(
        transaction: Transaction,
        transfer: Transfer,
        accountName: String
    ) {
        transaction.guid = UUID.randomUUID().toString()
        transaction.transactionDate = transfer.transactionDate
        transaction.description = "transfer withdrawal"
        transaction.category = "transfer"
        transaction.notes = "Transfer to ${transfer.destinationAccount}"
        transaction.amount = transfer.amount.negate()
        transaction.transactionState = TransactionState.Outstanding
        transaction.reoccurringType = ReoccurringType.Onetime
        transaction.accountType = AccountType.Debit
        transaction.accountNameOwner = accountName
        transaction.dateUpdated = Timestamp(Calendar.getInstance().time.time)
        transaction.dateAdded = Timestamp(Calendar.getInstance().time.time)
    }

    @Timed
    private fun populateDestinationTransaction(
        transaction: Transaction,
        transfer: Transfer,
        accountName: String
    ) {
        transaction.guid = UUID.randomUUID().toString()
        transaction.transactionDate = transfer.transactionDate
        transaction.description = "transfer deposit"
        transaction.category = "transfer"
        transaction.notes = "Transfer from ${transfer.sourceAccount}"
        transaction.amount = transfer.amount
        transaction.transactionState = TransactionState.Outstanding
        transaction.reoccurringType = ReoccurringType.Onetime
        transaction.accountType = AccountType.Debit
        transaction.accountNameOwner = accountName
        transaction.dateUpdated = Timestamp(Calendar.getInstance().time.time)
        transaction.dateAdded = Timestamp(Calendar.getInstance().time.time)
    }

    @Timed
    override fun deleteByTransferId(transferId: Long): Boolean {
        val transfer = transferRepository.findByTransferId(transferId).get()
        transferRepository.delete(transfer)
        return true
    }

    @Timed
    override fun findByTransferId(transferId: Long): Optional<Transfer> {
        logger.info("service - findByTransferId = $transferId")
        val transferOptional: Optional<Transfer> = transferRepository.findByTransferId(transferId)
        if (transferOptional.isPresent) {
            return transferOptional
        }
        return Optional.empty()
    }
}
