package finance.services

import finance.domain.*
import finance.repositories.TransactionRepository
import io.micrometer.core.annotation.Timed
import net.coobird.thumbnailator.Thumbnails
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Date
import java.sql.Timestamp
import java.util.*
import javax.imageio.IIOException
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import jakarta.validation.ConstraintViolation
import kotlin.system.measureTimeMillis

@Service
open class TransactionService(
    private var transactionRepository: TransactionRepository,
    private var accountService: AccountService,
    private var categoryService: CategoryService,
    private var descriptionService: DescriptionService,
    private var receiptImageService: ReceiptImageService
) : ITransactionService, BaseService() {

    @Timed
    override fun deleteTransactionByGuid(guid: String): Boolean {
        val transactionOptional: Optional<Transaction> = transactionRepository.findByGuid(guid)
        if (transactionOptional.isPresent) {
            val transaction = transactionOptional.get()
            transactionRepository.delete(transaction)
            //TODO: add metric here
            return true
        }
        //TODO: add metric here
        return false
    }

    @Timed
    override fun deleteReceiptImage(transaction: Transaction) : Boolean {
        val receiptImageId = transaction.receiptImageId ?: return false
        val receiptImageOptional = receiptImageService.findByReceiptImageId(receiptImageId)
        if (receiptImageOptional.isPresent) {
            receiptImageService.deleteReceiptImage(receiptImageOptional.get())
            //TODO: add metric here
            logger.info("deleted receipt image successfully.")
            return true
        }
        return false
    }

    // https://hornsup:8443/actuator/metrics/method.timed/?tag=method:insertTransaction
    @Timed
    override fun insertTransaction(transaction: Transaction): Transaction {
        val constraintViolations: Set<ConstraintViolation<Transaction>> = validator.validate(transaction)
        handleConstraintViolations(constraintViolations, meterService)
        val transactionOptional = findTransactionByGuid(transaction.guid)

        if (transactionOptional.isPresent) {
            val transactionFromDatabase = transactionOptional.get()
            meterService.incrementTransactionAlreadyExistsCounter(transactionFromDatabase.accountNameOwner)
            return masterTransactionUpdater(transactionFromDatabase, transaction)
        }

        processAccount(transaction)
        processCategory(transaction)
        processDescription(transaction)
        transaction.dateUpdated = Timestamp(nextTimestampMillis())
        transaction.dateAdded = Timestamp(nextTimestampMillis())
        val response: Transaction = transactionRepository.saveAndFlush(transaction)
        meterService.incrementTransactionSuccessfullyInsertedCounter(transaction.accountNameOwner)
        logger.info("Inserted transaction into the database successfully, guid = ${transaction.guid}")
        return response
    }

    @Timed
    override fun processAccount(transaction: Transaction) {
        var accountOptional = accountService.account(transaction.accountNameOwner)
        if (accountOptional.isPresent) {
            transaction.accountId = accountOptional.get().accountId
            transaction.accountType = accountOptional.get().accountType
            logger.info("METRIC_ACCOUNT_ALREADY_EXISTS_COUNTER")
        } else {
            val account = createDefaultAccount(transaction.accountNameOwner, transaction.accountType)
            accountService.insertAccount(account)
            //TODO: add metric here
            logger.info("inserted account from transactionService ${transaction.accountNameOwner}")
            accountOptional = accountService.account(transaction.accountNameOwner)
            transaction.accountId = accountOptional.get().accountId
            transaction.accountType = accountOptional.get().accountType
        }
    }

    @Timed
    override fun processCategory(transaction: Transaction) {
        when {
            transaction.category != "" -> {
                val optionalCategory = categoryService.category(transaction.category)
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

    override fun processDescription(transaction: Transaction) {
        when {
            transaction.description != "" -> {
                val optionalDescription = descriptionService.description(transaction.description)
                if (!optionalDescription.isPresent) {
                    val description = createDefaultDescription(transaction.description)
                    descriptionService.insertDescription(description)
                    logger.info("inserted description from transactionService ${transaction.description}")
                }
            }
        }
    }

    @Timed
    override fun createDefaultCategory(categoryName: String): Category {
        val category = Category()

        category.categoryName = categoryName
        return category
    }

    @Timed
    override fun createDefaultDescription(descriptionName: String): Description {
        val description = Description()

        description.descriptionName = descriptionName
        return description
    }

    @Timed
    override fun createDefaultAccount(accountNameOwner: String, accountType: AccountType): Account {
        val account = Account()

        account.accountNameOwner = accountNameOwner
        account.moniker = "0000"
        account.accountType = accountType
        account.activeStatus = true
        return account
    }

    override fun findTransactionsByCategory(categoryName: String): List<Transaction> {
        val transactions = transactionRepository.findByCategoryAndActiveStatusOrderByTransactionDateDesc(categoryName)

        // If the list is empty, you can return an empty list or handle it in another way if needed
        return transactions.ifEmpty { emptyList() }
    }

    override fun findTransactionsByDescription(descriptionName: String): List<Transaction> {
        val transactions = transactionRepository.findByDescriptionAndActiveStatusOrderByTransactionDateDesc(descriptionName)

        // If the list is empty, you can return an empty list or handle it in another way if needed
        return transactions.ifEmpty { emptyList() }
    }

    @Timed
    override fun findTransactionByGuid(guid: String): Optional<Transaction> {
        val transactionOptional: Optional<Transaction> = transactionRepository.findByGuid(guid)
        if (transactionOptional.isPresent) {
            return transactionOptional
        }
        return Optional.empty()
    }

    @Timed
    override fun calculateActiveTotalsByAccountNameOwner(accountNameOwner: String): Totals {
        var resultSet: List<Any>

        val queryTimeInMillis = measureTimeMillis {
            resultSet = transactionRepository.sumTotalsForActiveTransactionsByAccountNameOwner(accountNameOwner)
        }
        logger.info("The query took $queryTimeInMillis ms")

        var totalsFuture = BigDecimal.ZERO
        var totalsCleared = BigDecimal.ZERO
        var totalsOutstanding = BigDecimal.ZERO


        resultSet.forEach { row ->
            val rowList = row as Array<*>
            val totals = BigDecimal(rowList[0].toString())
            when (val transactionState = rowList[2].toString()) {
                "future" -> totalsFuture = totals.setScale(2, RoundingMode.HALF_UP)
                "cleared" -> totalsCleared = totals.setScale(2, RoundingMode.HALF_UP)
                "outstanding" -> totalsOutstanding = totals.setScale(2, RoundingMode.HALF_UP)
                else -> {
                    // Handle unexpected transaction states.  Log an error, throw an exception, or ignore.
                    logger.warn("Unexpected transaction state: $transactionState")
                }
            }
        }

        val grandTotal = (totalsFuture + totalsCleared + totalsOutstanding).setScale(2, RoundingMode.HALF_UP)

        return Totals(
            totalsFuture = totalsFuture,
            totalsCleared = totalsCleared,
            totals = grandTotal,  // Correctly set the grand total
            totalsOutstanding = totalsOutstanding
        )
    }

    @Timed
    override fun findByAccountNameOwnerOrderByTransactionDate(accountNameOwner: String): List<Transaction> {
        val transactions = transactionRepository.findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc(accountNameOwner)

        // Early return if transactions list is empty
        if (transactions.isEmpty()) {
            logger.error("No active transactions found for account owner: $accountNameOwner")
            meterService.incrementAccountListIsEmpty("non-existent-accounts")
            return emptyList()  // Return early to avoid unnecessary sorting
        }

        // Sort only if there are transactions
        return transactions.sortedWith(
            compareByDescending<Transaction> { it.transactionState }
                .thenByDescending { it.transactionDate }
        )
    }

    @Timed
    override fun updateTransaction(transaction: Transaction): Transaction {
        val constraintViolations: Set<ConstraintViolation<Transaction>> = validator.validate(transaction)
        handleConstraintViolations(constraintViolations, meterService)
        val optionalTransaction = transactionRepository.findByGuid(transaction.guid)
        if (optionalTransaction.isPresent) {
            val transactionFromDatabase = optionalTransaction.get()
            return masterTransactionUpdater(transactionFromDatabase, transaction)
        }
        logger.warn("cannot update a transaction without a valid guid.")
        throw RuntimeException("cannot update a transaction without a valid guid.")
    }

    @Timed
    override fun masterTransactionUpdater(transactionFromDatabase: Transaction, transaction: Transaction): Transaction {

        if (transactionFromDatabase.guid == transaction.guid) {
            processCategory(transaction)
            val account = accountService.account(transaction.accountNameOwner).get()
            transaction.accountId = account.accountId
            transaction.dateAdded = transactionFromDatabase.dateAdded
            transaction.dateUpdated = Timestamp(Calendar.getInstance().time.time)
            processDescription(transaction)
            return transactionRepository.saveAndFlush(transaction)
        }
        logger.warn("guid did not match any database records to update ${transaction.guid}.")
        throw RuntimeException("guid did not match any database records to update ${transaction.guid}.")
    }

    @Timed
    override fun updateTransactionReceiptImageByGuid(guid: String, imageBase64Payload: String): ReceiptImage {
        val imageBase64String = imageBase64Payload.replace("^data:image/[a-z]+;base64,[ ]?".toRegex(), "")
        //val rawImage = Base64Utils.decodeFromString(imageBase64String)
        val rawImage = Base64.getDecoder().decode(imageBase64String)
        val imageFormatType = getImageFormatType(rawImage)
        val thumbnail = createThumbnail(rawImage, imageFormatType)
        val optionalTransaction = transactionRepository.findByGuid(guid)
        if (optionalTransaction.isPresent) {
            val transaction = optionalTransaction.get()

            logger.info("receiptImageId: ${transaction.receiptImageId}")
            if (transaction.receiptImageId != null) {
                val receiptImageOptional = receiptImageService.findByReceiptImageId(transaction.receiptImageId!!)
                if (receiptImageOptional.isPresent) {
                    val existingReceiptImage = receiptImageOptional.get()
                    existingReceiptImage.thumbnail = thumbnail
                    existingReceiptImage.image = rawImage
                    existingReceiptImage.imageFormatType = imageFormatType
                    return receiptImageService.insertReceiptImage(receiptImageOptional.get())
                }
                logger.error("Failed to update receipt image for transaction ${transaction.guid}")
                meterService.incrementExceptionThrownCounter("RuntimeException")
                throw RuntimeException("Failed to update receipt image for transaction ${transaction.guid}")
            }
            logger.info("added new receipt image: ${transaction.transactionId}")
            val receiptImage = ReceiptImage()
            receiptImage.transactionId = transaction.transactionId
            receiptImage.image = rawImage
            receiptImage.thumbnail = thumbnail
            receiptImage.imageFormatType = imageFormatType
            val response = receiptImageService.insertReceiptImage(receiptImage)
            transaction.receiptImageId = response.receiptImageId
            transaction.dateUpdated = Timestamp(nextTimestampMillis())
            transactionRepository.saveAndFlush(transaction)
            meterService.incrementTransactionReceiptImageInserted(transaction.accountNameOwner)
            return response
        }
        //TODO: add metric here
        logger.error("Cannot save a image for a transaction that does not exist with guid = '${guid}'.")
        meterService.incrementExceptionThrownCounter("RuntimeException")
        throw RuntimeException("Cannot save a image for a transaction that does not exist with guid = '${guid}'.")
    }

    @Timed
    override fun changeAccountNameOwner(map: Map<String, String>): Transaction {
        val accountNameOwner = map["accountNameOwner"]
        val guid = map["guid"]

        if (guid != null && accountNameOwner != null) {
            val accountOptional = accountService.account(accountNameOwner)
            val transactionOptional = findTransactionByGuid(guid)

            if (transactionOptional.isPresent && accountOptional.isPresent) {
                val account = accountOptional.get()
                val transaction = transactionOptional.get()
                transaction.accountNameOwner = account.accountNameOwner
                transaction.accountId = account.accountId
                transaction.dateUpdated = Timestamp(nextTimestampMillis())
                return transactionRepository.saveAndFlush(transaction)
            } else {
                //TODO: add metric here
                logger.error("Cannot change accountNameOwner for a transaction that does not exist, guid='${guid}'.")
                throw RuntimeException("Cannot change accountNameOwner for a transaction that does not exist, guid='${guid}'.")
            }
        }
        //TODO: add metric here
        logger.error("Cannot change accountNameOwner for an input that has a null 'accountNameOwner' or a null 'guid'")
        meterService.incrementExceptionThrownCounter("RuntimeException")
        throw RuntimeException("Cannot change accountNameOwner for an input that has a null 'accountNameOwner' or a null 'guid'")
    }

    @Timed
    override fun updateTransactionState(guid: String, transactionState: TransactionState): Transaction {
        val transactionOptional = findTransactionByGuid(guid)
        if (transactionOptional.isPresent) {
            //val transactions = mutableListOf<Transaction>()
            val transaction = transactionOptional.get()
            if (transactionState == transaction.transactionState) {
                logger.error("Cannot update transactionState to the same for guid = '${guid}'")
                throw RuntimeException("Cannot update transactionState to the same for guid = '${guid}'")
            }
            if (transactionState == TransactionState.Cleared &&
                transaction.transactionDate > Date(Calendar.getInstance().timeInMillis)
            ) {
                logger.error("Cannot set cleared status on a future dated transaction: ${transaction.transactionDate}.")
                meterService.incrementExceptionThrownCounter("RuntimeException")
                throw RuntimeException("Cannot set cleared status on a future dated transaction: ${transaction.transactionDate}.")
            }
            meterService.incrementTransactionUpdateClearedCounter(transaction.accountNameOwner)
            transaction.transactionState = transactionState
            transaction.dateUpdated = Timestamp(nextTimestampMillis())
            return transactionRepository.saveAndFlush(transaction)
        }
        //TODO: add metric here
        logger.error("Cannot update transaction - the transaction is not found with guid = '${guid}'")
        meterService.incrementExceptionThrownCounter("RuntimeException")
        throw RuntimeException("Cannot update transaction - the transaction is not found with guid = '${guid}'")
    }

    @Timed
    override fun createThumbnail(rawImage: ByteArray, imageFormatType: ImageFormatType): ByteArray {
        try {
            val bufferedImage = ImageIO.read(ByteArrayInputStream(rawImage))

            val thumbnail = Thumbnails.of(bufferedImage).size(100, 100).asBufferedImage()

            val byteArrayOutputStream = ByteArrayOutputStream()
            ImageIO.write(thumbnail, imageFormatType.toString(), byteArrayOutputStream)
            return byteArrayOutputStream.toByteArray()
        } catch (iIOException: IIOException) {
            logger.warn("IIOException, ${iIOException.message}")
            //TODO: need to fix the cause of this exception
            meterService.incrementExceptionCaughtCounter("IIOException")
        }
        return byteArrayOf()
    }

    @Timed
    override fun getImageFormatType(rawImage: ByteArray): ImageFormatType {
        val imageInputStream = ImageIO.createImageInputStream(ByteArrayInputStream(rawImage))
        val imageReaders: Iterator<ImageReader> = ImageIO.getImageReaders(imageInputStream)
        var format = ImageFormatType.Undefined

        imageReaders.forEachRemaining { imageReader ->
            format = when {
                imageReader.formatName.lowercase() == "jpeg" -> {
                    logger.info(imageReader.formatName)
                    ImageFormatType.Jpeg
                }
                imageReader.formatName.lowercase() == "png" -> {
                    logger.info(imageReader.formatName)
                    ImageFormatType.Png
                }
                else -> {
                    ImageFormatType.Undefined
                }
            }
        }
        return format
    }

    @Timed
    override fun createFutureTransaction(transaction: Transaction): Transaction {
        val calendarTransactionDate = Calendar.getInstance()
        val calendarDueDate = Calendar.getInstance()
        calendarTransactionDate.time = transaction.transactionDate
        calculateFutureDate(transaction, calendarTransactionDate)
        val transactionFuture = Transaction()

        if(  transaction.dueDate != null ) {
            calendarDueDate.time = transaction.dueDate
            calculateFutureDate(transaction, calendarDueDate)
            transactionFuture.dueDate = Date(calendarDueDate.timeInMillis)
        }

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
        transactionFuture.reoccurringType = transaction.reoccurringType
        transactionFuture.transactionState = TransactionState.Future
        transactionFuture.transactionDate = Date(calendarTransactionDate.timeInMillis)
        transactionFuture.dateUpdated = Timestamp(nextTimestampMillis())
        transactionFuture.dateAdded = Timestamp(nextTimestampMillis())
        logger.info(transactionFuture.toString())
        if (transactionFuture.reoccurringType == ReoccurringType.Undefined) {
            logger.error("TransactionState cannot be undefined for reoccurring transactions.")
            meterService.incrementExceptionThrownCounter("RuntimeException")
            throw RuntimeException("TransactionState cannot be undefined for reoccurring transactions.")
        }
        return transactionFuture
    }

    private fun calculateFutureDate(transaction: Transaction, calendar: Calendar) {
        if (transaction.reoccurringType == ReoccurringType.FortNightly) {
            calendar.add(Calendar.DATE, 14)
        } else {
            if (transaction.accountType == AccountType.Debit) {
                if (transaction.reoccurringType == ReoccurringType.Monthly) {
                    calendar.add(Calendar.MONTH, 1)
                } else {
                    logger.warn("debit transaction ReoccurringType needs to be configured.")
                    throw java.lang.RuntimeException("debit transaction ReoccurringType needs to be configured.")
                }
            } else {
                calendar.add(Calendar.YEAR, 1) //Assumption this method works for leap years
            }
        }
    }

//    @Timed
//    override fun findAccountsThatRequirePayment(): List<Account> {
//        val calendar = Calendar.getInstance()
//        calendar.add(Calendar.DAY_OF_MONTH, 30)
//        val accountNeedingAttention = mutableListOf<Account>()
//        accountService.updateTotalsForAllAccounts()
//        val accountsToInvestigate =
//            accountService.findByActiveStatusAndAccountTypeAndTotalsIsGreaterThanOrderByAccountNameOwner()
//        accountsToInvestigate.forEach { account ->
//
//            accountNeedingAttention.add(account)
//        }
//
//        if (accountNeedingAttention.isNotEmpty()) {
//            logger.info("accountNeedingAttention={${accountNeedingAttention.size}}")
//        }
//        return accountNeedingAttention
//    }

    @Timed
    override fun nextTimestampMillis(): Long {
        val lastTimestamp = System.currentTimeMillis()
        var timestamp = System.currentTimeMillis()
        while (timestamp < lastTimestamp) {
            timestamp = System.currentTimeMillis()
        }
        return timestamp
    }
}
