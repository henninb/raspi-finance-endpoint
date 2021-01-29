package finance.services

import finance.domain.*
import java.math.BigDecimal
import java.util.*

interface ITransactionService {
    fun deleteTransactionByGuid(guid: String): Boolean
    fun deleteReceiptImage(transaction: Transaction)
    fun insertTransaction(transaction: Transaction): Boolean
    fun processAccount(transaction: Transaction)
    fun processCategory(transaction: Transaction)
    fun createDefaultCategory(categoryName: String): Category
    fun createDefaultAccount(accountNameOwner: String, accountType: AccountType): Account
    fun findTransactionByGuid(guid: String): Optional<Transaction>
    fun fetchTotalsByAccountNameOwner(accountNameOwner: String): Map<String, BigDecimal>
    fun findByAccountNameOwnerOrderByTransactionDate(accountNameOwner: String): List<Transaction>
    fun updateTransaction(transaction: Transaction): Boolean
    fun masterTransactionUpdater(transactionFromDatabase: Transaction, transaction: Transaction): Boolean
    fun updateTransactionReceiptImageByGuid(guid: String, imageBase64Payload: String): ReceiptImage
    fun changeAccountNameOwner(map: Map<String, String>): Boolean
    fun updateTransactionState(guid: String, transactionState: TransactionState): MutableList<Transaction>
    fun createThumbnail(rawImage: ByteArray, imageFormatType: ImageFormatType): ByteArray
    fun getImageFormatType(rawImage: ByteArray): ImageFormatType
    fun createFutureTransaction(transaction: Transaction): Transaction
    fun updateTransactionReoccurringFlag(guid: String, reoccurring: Boolean): Boolean
    fun findAccountsThatRequirePayment(): List<Account>
    fun nextTimestampMillis(): Long
}