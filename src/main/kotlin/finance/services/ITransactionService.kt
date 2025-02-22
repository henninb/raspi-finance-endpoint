package finance.services

import finance.domain.*
import java.math.BigDecimal
import java.util.*

interface ITransactionService {
    fun deleteTransactionByGuid(guid: String): Boolean
    fun deleteReceiptImage(transaction: Transaction) : Boolean
    fun insertTransaction(transaction: Transaction): Transaction
    fun processAccount(transaction: Transaction)
    fun processCategory(transaction: Transaction)
    fun createDefaultCategory(categoryName: String): Category
    fun createDefaultAccount(accountNameOwner: String, accountType: AccountType): Account
    fun findTransactionByGuid(guid: String): Optional<Transaction>
    fun calculateActiveTotalsByAccountNameOwner(accountNameOwner: String): Totals
    fun findByAccountNameOwnerOrderByTransactionDate(accountNameOwner: String): List<Transaction>
    fun updateTransaction(transaction: Transaction): Transaction
    fun masterTransactionUpdater(transactionFromDatabase: Transaction, transaction: Transaction): Transaction
    fun updateTransactionReceiptImageByGuid(guid: String, imageBase64Payload: String): ReceiptImage
    fun changeAccountNameOwner(map: Map<String, String>): Transaction
    fun updateTransactionState(guid: String, transactionState: TransactionState): Transaction
    fun createThumbnail(rawImage: ByteArray, imageFormatType: ImageFormatType): ByteArray
    fun getImageFormatType(rawImage: ByteArray): ImageFormatType
    fun createFutureTransaction(transaction: Transaction): Transaction
    //fun findAccountsThatRequirePayment(): List<Account>
    fun nextTimestampMillis(): Long
    fun createDefaultDescription(descriptionName: String): Description
    fun processDescription(transaction: Transaction)
    fun findTransactionsByCategory(categoryName: String): List<Transaction>
    fun findTransactionsByDescription(descriptionName: String): List<Transaction>
}