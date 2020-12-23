package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.*
import finance.repositories.TransactionRepository
import io.micrometer.core.annotation.Timed
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Date
import java.util.*
import javax.validation.ConstraintViolation
import javax.validation.ValidationException
import javax.validation.Validator

@Service
open class TransactionService @Autowired constructor(
    private var transactionRepository: TransactionRepository,
    private var accountService: AccountService,
    private var categoryService: CategoryService,
    private var receiptImageService: ReceiptImageService,
    private val validator: Validator,
    private var meterService: MeterService
) {

    @Timed
    @Transactional
    open fun deleteTransactionByGuid(guid: String): Boolean {
        val transactionOptional: Optional<Transaction> = transactionRepository.findByGuid(guid)
        if (transactionOptional.isPresent) {
            val transaction = transactionOptional.get()
            if (transaction.categories.size > 0) {
                //TODO: add metric here
                val categoryOptional = categoryService.findByCategory(transaction.category)
                transaction.categories.remove(categoryOptional.get())
            }

            if (transaction.receiptImageId != null) {
                deleteReceiptImage(transaction)
                transaction.receiptImageId = null
            }

            transactionRepository.deleteByGuid(guid)
            //TODO: add metric here
            return true
        }
        //TODO: add metric here
        return false
    }

    private fun deleteReceiptImage(transaction: Transaction) {
        val receiptImageOptional = receiptImageService.findByReceiptImageId(transaction.receiptImageId!!)
        if (receiptImageOptional.isPresent) {
            receiptImageService.deleteReceiptImage(receiptImageOptional.get())
            //TODO: add metric here
        }
    }

    //https://hornsup:8080/actuator/metrics/method.timed/?tag=method:insertTransaction
    @Timed
    @Transactional
    open fun insertTransaction(transaction: Transaction): Boolean {
        val constraintViolations: Set<ConstraintViolation<Transaction>> = validator.validate(transaction)
        if (constraintViolations.isNotEmpty()) {
            //TODO: add metric here
            logger.error("Cannot insert transaction as there is a constraint violation on the data.")
            throw ValidationException("Cannot insert transaction as there is a constraint violation on the data.")
        }
        val transactionOptional = findTransactionByGuid(transaction.guid)

        if (transactionOptional.isPresent) {
            val transactionDb = transactionOptional.get()
            return updateTransaction(transactionDb, transaction)
        }

        processAccount(transaction)
        processCategory(transaction)
        transactionRepository.saveAndFlush(transaction)
        meterService.incrementTransactionSuccessfullyInsertedCounter(transaction.accountNameOwner)
        logger.info("Inserted transaction into the database successfully, guid = ${transaction.guid}")
        return true
    }

    private fun processAccount(transaction: Transaction) {
        var accountOptional = accountService.findByAccountNameOwner(transaction.accountNameOwner)
        if (accountOptional.isPresent) {
            transaction.accountId = accountOptional.get().accountId
            transaction.accountType = accountOptional.get().accountType
            logger.info("METRIC_ACCOUNT_ALREADY_EXISTS_COUNTER")
        } else {
            val account = createDefaultAccount(transaction.accountNameOwner, transaction.accountType)
            accountService.insertAccount(account)
            //TODO: add metric here
            logger.info("inserted account from transactionService ${transaction.accountNameOwner}")
            accountOptional = accountService.findByAccountNameOwner(transaction.accountNameOwner)
            transaction.accountId = accountOptional.get().accountId
            transaction.accountType = accountOptional.get().accountType
        }
    }

    private fun processCategory(transaction: Transaction) {
        when {
            transaction.category != "" -> {
                val optionalCategory = categoryService.findByCategory(transaction.category)
                if (optionalCategory.isPresent) {
                    transaction.categories.add(optionalCategory.get())
                } else {
                    val category = createDefaultCategory(transaction.category)
                    categoryService.insertCategory(category)
                    logger.info("inserted category from transactionService ${transaction.category}")
                    transaction.categories.add(category)
                }
            }
        }
    }

    private fun updateTransaction(transactionDb: Transaction, transaction: Transaction): Boolean {
        if (transactionDb.accountNameOwner.trim() == transaction.accountNameOwner) {

            if (transactionDb.amount != transaction.amount) {
                logger.info("discrepancy in the amount for <${transactionDb.guid}>")
                //TODO: add metric here
                transactionRepository.setAmountByGuid(transaction.amount, transaction.guid)
                return true
            }

            if (transactionDb.transactionState != transaction.transactionState) {
                logger.info("discrepancy in the cleared value for <${transactionDb.guid}>")
                //TODO: add metric here
                transactionRepository.setTransactionStateByGuid(transaction.transactionState, transaction.guid)
                return true
            }
        }

        //TODO: add metric here
        logger.info("transaction already exists, no transaction data inserted for ${transaction.guid}")
        return false
    }

    private fun createDefaultCategory(categoryName: String): Category {
        val category = Category()

        category.category = categoryName
        return category
    }

    private fun createDefaultAccount(accountNameOwner: String, accountType: AccountType): Account {
        val account = Account()

        account.accountNameOwner = accountNameOwner
        account.moniker = "0000"
        account.accountType = accountType
        account.activeStatus = true
        return account
    }

    @Timed
    @Transactional
    open fun findTransactionByGuid(guid: String): Optional<Transaction> {
        val transactionOptional: Optional<Transaction> = transactionRepository.findByGuid(guid)
        if (transactionOptional.isPresent) {
            return transactionOptional
        }
        return Optional.empty()
    }

    @Timed
    @Transactional
    open fun fetchTotalsByAccountNameOwner(accountNameOwner: String): Map<String, BigDecimal> {

        val result: MutableMap<String, BigDecimal> = HashMap()
        val totalsCleared = retrieveTotalsCleared(accountNameOwner)
        val totals = retrieveTotals(accountNameOwner)

        result["totals"] = BigDecimal(totals).setScale(2, RoundingMode.HALF_UP)
        result["totalsCleared"] = BigDecimal(totalsCleared).setScale(2, RoundingMode.HALF_UP)
        return result
    }

    private fun retrieveTotals(accountNameOwner: String): Double {
        try {
            return transactionRepository.getTotalsByAccountNameOwner(accountNameOwner)
        } catch (e: Exception) {
            meterService.incrementExceptionCounter(e.toString())
            logger.error("empty getTotalsByAccountNameOwner failed.")
        }
        return 0.00
    }

    private fun retrieveTotalsCleared(accountNameOwner: String): Double {
        try {
            return transactionRepository.getTotalsByAccountNameOwnerTransactionState(accountNameOwner)
        } catch (e: Exception) {
            meterService.incrementExceptionCounter(e.toString())
            logger.error("empty getTotalsByAccountNameOwnerCleared failed.")
        }
        return 0.00
    }

    @Timed
    @Transactional
    open fun findByAccountNameOwnerIgnoreCaseOrderByTransactionDate(accountNameOwner: String): List<Transaction> {
        val transactions: List<Transaction> =
            transactionRepository.findByAccountNameOwnerIgnoreCaseOrderByTransactionDateDesc(accountNameOwner)
        //TODO: look into this type of error handling

        val sortedTransactions =
            transactions.sortedWith(compareByDescending<Transaction> { it.transactionState }.thenByDescending { it.transactionDate })
        if (transactions.isEmpty()) {
            logger.error("an empty list of AccountNameOwner.")
            meterService.incrementAccountListIsEmpty("non-existent-accounts")
        }
        return sortedTransactions
    }

    @Timed
    @Transactional
    open fun updateTransaction(transaction: Transaction): Boolean {
        val constraintViolations: Set<ConstraintViolation<Transaction>> = validator.validate(transaction)
        if (constraintViolations.isNotEmpty()) {
            logger.error("Cannot update transaction as there is a constraint violation on the data.")
            throw ValidationException("Cannot update transaction as there is a constraint violation on the data.")
        }
        val optionalTransaction = transactionRepository.findByGuid(transaction.guid)
        //TODO: add logic for patch
        if (optionalTransaction.isPresent) {
            val fromDb = optionalTransaction.get()
            if (fromDb.guid == transaction.guid) {
                logger.info("successful patch $transaction")
                processCategory(transaction)
                transactionRepository.saveAndFlush(transaction)
                if (transaction.transactionState == TransactionState.Cleared &&
                    transaction.reoccurring == true
                    && transaction.reoccurringType != ReoccurringType.Undefined
                ) {
                    transactionRepository.saveAndFlush(createFutureTransaction(transaction))
                }
            } else {
                logger.warn("GUID did not match any database records.")
                return false
            }
        } else {
            logger.warn("WARN: cannot patch a transaction without a valid GUID.")
            return false
        }
        return true
    }

    @Timed
    @Transactional
    open fun cloneAsMonthlyTransaction(map: Map<String, String>): Boolean {
        val guid: String = map["guid"] ?: error("guid must be set.")
        val amount: String = map["amount"] ?: error("transactionDate must be set.")
        val isMonthEnd = map["monthEnd"] ?: error("monthEnd must be set.")
        val specificDay = map["specificDay"] ?: error("specificDay must be set.")

        val optionalTransaction = transactionRepository.findByGuid(guid)

        val calendar = Calendar.getInstance()
        val month = calendar[Calendar.MONTH]
        val year = calendar[Calendar.YEAR]
        calendar.clear()
        calendar[Calendar.YEAR] = year

        for (currentMonth in month..11) {
            calendar[Calendar.MONTH] = currentMonth

            val fixedMonthDay: Date = calculateDayOfTheMonth(isMonthEnd, calendar, specificDay)

            if (optionalTransaction.isPresent) {
                setValuesForClearedReoccurringTransactions(optionalTransaction, fixedMonthDay, amount)
            } else {
                logger.error("Cannot clone monthly transaction for a record found '${guid}'.")
                throw RuntimeException("Cannot clone monthly transaction for a record found '${guid}'.")
            }
        }
        return true
    }

    @Timed
    @Transactional
    open fun updateTransactionReceiptImageByGuid(guid: String, jpgBase64Data: ByteArray): Boolean {
        val optionalTransaction = transactionRepository.findByGuid(guid)
        if (optionalTransaction.isPresent) {
            val transaction = optionalTransaction.get()

            logger.info("receiptImageId: ${transaction.receiptImageId}")
            if (transaction.receiptImageId != null) {
                logger.info("update existing receipt image: ${transaction.transactionId}")
                val receiptImageOptional = receiptImageService.findByReceiptImageId(transaction.receiptImageId!!)
                if (receiptImageOptional.isPresent) {
                    receiptImageOptional.get().jpgImage = jpgBase64Data
                    receiptImageService.insertReceiptImage(receiptImageOptional.get())
                } else {
                    throw RuntimeException("failed to update receipt image for transaction ${transaction.guid}")
                }

                meterService.incrementTransactionReceiptImage(transaction.accountNameOwner)
                return true
            }
            logger.info("added new receipt image: ${transaction.transactionId}")
            val receiptImage = ReceiptImage()
            receiptImage.transactionId = transaction.transactionId
            receiptImage.jpgImage = jpgBase64Data
            val receiptImageId = receiptImageService.insertReceiptImage(receiptImage)
            transaction.receiptImageId = receiptImageId

            transactionRepository.saveAndFlush(transaction)
            meterService.incrementTransactionReceiptImage(transaction.accountNameOwner)
            return true
        }
        //TODO: add metric here
        logger.error("Cannot save a image for a transaction that does not exist with guid = '${guid}'.")
        throw RuntimeException("Cannot save a image for a transaction that does not exist with guid = '${guid}'.")
    }

    private fun setValuesForClearedReoccurringTransactions(
        optionalTransaction: Optional<Transaction>,
        fixedMonthDay: Date,
        amount: String
    ): Boolean {
        val oldTransaction = optionalTransaction.get()
        val transaction = Transaction()
        transaction.guid = UUID.randomUUID().toString()
        transaction.transactionDate = fixedMonthDay
        transaction.description = oldTransaction.description
        transaction.category = oldTransaction.category
        transaction.amount = amount.toBigDecimal()
        transaction.transactionState = TransactionState.Future
        transaction.notes = oldTransaction.notes
        transaction.reoccurring = oldTransaction.reoccurring
        transaction.accountType = oldTransaction.accountType
        transaction.accountId = oldTransaction.accountId
        transaction.accountNameOwner = oldTransaction.accountNameOwner
        transactionRepository.saveAndFlush(transaction)
        return true
    }

    private fun calculateDayOfTheMonth(isMonthEnd: String, calendar: Calendar, specificDay: String): Date {
        if (isMonthEnd.toBoolean()) {
            calendar[Calendar.DAY_OF_MONTH] = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        } else {
            calendar[Calendar.DAY_OF_MONTH] = specificDay.toInt()
        }
        val calendarDate = calendar.time
        return Date(calendarDate.time)
    }

    @Timed
    @Transactional
    open fun changeAccountNameOwner(map: Map<String, String>): Boolean {
        val accountNameOwner = map["accountNameOwner"]
        val guid = map["guid"]

        if (guid != null && accountNameOwner != null) {
            val accountOptional = accountService.findByAccountNameOwner(accountNameOwner)
            val transactionOptional = findTransactionByGuid(guid)

            if (transactionOptional.isPresent && accountOptional.isPresent) {
                val account = accountOptional.get()
                val transaction = transactionOptional.get()
                transaction.accountNameOwner = account.accountNameOwner
                transaction.accountId = account.accountId
                transactionRepository.saveAndFlush(transaction)
                return true
            } else {
                //TODO: add metric here
                logger.error("Cannot change accountNameOwner for a transaction that does not exist, guid='${guid}'.")
                throw RuntimeException("Cannot change accountNameOwner for a transaction that does not exist, guid='${guid}'.")
            }
        }
        //TODO: add metric here
        logger.error("Cannot change accountNameOwner for an input that has a null 'accountNameOwner' or a null 'guid'")
        throw RuntimeException("Cannot change accountNameOwner for an input that has a null 'accountNameOwner' or a null 'guid'")
    }

    @Timed
    @Transactional
    open fun updateTransactionState(guid: String, transactionState: TransactionState): MutableList<Transaction> {
        val transactionOptional = findTransactionByGuid(guid)
        if (transactionOptional.isPresent) {
            val transactions = mutableListOf<Transaction>()
            val transaction = transactionOptional.get()

            transaction.transactionState = transactionState
            val databaseResponseUpdated = transactionRepository.saveAndFlush(transaction)
            transactions.add(databaseResponseUpdated)
            //TODO: add metric here
            if (databaseResponseUpdated.transactionState == TransactionState.Cleared &&
                databaseResponseUpdated.reoccurring == true
                && databaseResponseUpdated.reoccurringType != ReoccurringType.Undefined
            ) {
                val databaseResponseInserted = transactionRepository.saveAndFlush(createFutureTransaction(transaction))
                transactions.add(databaseResponseInserted)
                //TODO: add metric here
            }
            return transactions
        }
        //TODO: add metric here
        logger.error("Cannot update transaction - the transaction is not found with guid = '${guid}'")
        throw RuntimeException("Cannot update transaction - the transaction is not found with guid = '${guid}'")
    }

    private fun createFutureTransaction(transaction: Transaction): Transaction {
        val calendar = Calendar.getInstance()
        calendar.time = transaction.transactionDate

        if( transaction.reoccurringType == ReoccurringType.FortNightly) {
            calendar.add(Calendar.DATE, 14)
        } else {
            calendar.add(Calendar.YEAR, 1)
        }

        val transactionFuture = Transaction()
        transactionFuture.guid = UUID.randomUUID().toString()
        transactionFuture.account = transaction.account
        transactionFuture.accountId = transaction.accountId
        transactionFuture.accountNameOwner = transaction.accountNameOwner
        transactionFuture.accountType = transaction.accountType
        transactionFuture.activeStatus = transaction.activeStatus
        transactionFuture.amount = transaction.amount
        transactionFuture.category = transaction.category
        transactionFuture.description = transaction.description
        transactionFuture.receiptImageId = null
        transactionFuture.notes = ""
        transactionFuture.reoccurring = true
        transactionFuture.reoccurringType = transaction.reoccurringType
        transactionFuture.transactionState = TransactionState.Future
        transactionFuture.transactionDate = Date(calendar.timeInMillis)
        logger.info("1 future reoccurringType=" + transactionFuture.reoccurringType)
        logger.info(transactionFuture.toString())
        if (transactionFuture.reoccurringType == ReoccurringType.Undefined ) {
            throw java.lang.RuntimeException("transaction state cannot be undefined for reoccurring transactions.")
        }
        return transactionFuture
    }

    @Timed
    @Transactional
    open fun updateTransactionReoccurringFlag(guid: String, reoccurring: Boolean): Boolean {
        val transactionOptional = findTransactionByGuid(guid)
        if (transactionOptional.isPresent) {
            val transaction = transactionOptional.get()
            transaction.reoccurring = reoccurring
            transactionRepository.saveAndFlush(transaction)
            //TODO: add metric here
            return true
        }
        //TODO: add metric here
        logger.error("Cannot update transaction reoccurring state - the transaction is not found with guid = '${guid}'")
        throw RuntimeException("Cannot update transaction reoccurring state - the transaction is not found with guid = '${guid}'")
    }

    companion object {
        private val mapper = ObjectMapper()
        private val logger = LogManager.getLogger()
    }
}
