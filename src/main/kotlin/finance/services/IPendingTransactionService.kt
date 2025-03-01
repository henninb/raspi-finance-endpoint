package finance.services

import finance.domain.PendingTransaction
import java.util.*

interface IPendingTransactionService {
    fun insertPendingTransaction(pendingTransaction: PendingTransaction): PendingTransaction

    fun deletePendingTransaction(pendingTransactionId: Long): Boolean

    fun getAllPendingTransactions(): List<PendingTransaction>


//    fun updatePendingTransaction(pendingTransaction: PendingTransaction): PendingTransaction
//
//    fun approvePendingTransaction(pendingTransactionId: Long): PendingTransaction
//
//    fun rejectPendingTransaction(pendingTransactionId: Long): PendingTransaction
}