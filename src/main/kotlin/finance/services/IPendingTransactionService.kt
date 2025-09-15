package finance.services

import finance.domain.PendingTransaction
import java.util.*

interface IPendingTransactionService {
    fun insertPendingTransaction(pendingTransaction: PendingTransaction): PendingTransaction

    fun deletePendingTransaction(pendingTransactionId: Long): Boolean

    fun getAllPendingTransactions(): List<PendingTransaction>
    fun deleteAllPendingTransactions(): Boolean

    // Added for standardized controller support
    fun findByPendingTransactionId(pendingTransactionId: Long): Optional<PendingTransaction>
    fun updatePendingTransaction(pendingTransaction: PendingTransaction): PendingTransaction

    // Future enhancements (commented for now)
    // fun approvePendingTransaction(pendingTransactionId: Long): PendingTransaction
    // fun rejectPendingTransaction(pendingTransactionId: Long): PendingTransaction
}