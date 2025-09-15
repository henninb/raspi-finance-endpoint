package finance.services

import finance.domain.PendingTransaction
import finance.repositories.PendingTransactionRepository
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.util.*


@Service
open class PendingTransactionService(
        private var pendingTransactionRepository: PendingTransactionRepository,
    ) : IPendingTransactionService, BaseService() {


    override fun insertPendingTransaction(pendingTransaction: PendingTransaction): PendingTransaction {
        pendingTransaction.dateAdded = Timestamp(Calendar.getInstance().time.time)
        return pendingTransactionRepository.saveAndFlush(pendingTransaction)
    }

    override fun deletePendingTransaction(pendingTransactionId: Long): Boolean {
        val category = pendingTransactionRepository.findByPendingTransactionIdOrderByTransactionDateDesc(pendingTransactionId).get()
        pendingTransactionRepository.delete(category)
        return true
    }

    override fun getAllPendingTransactions(): List<PendingTransaction> {
        return pendingTransactionRepository.findAll()
    }

    override fun deleteAllPendingTransactions(): Boolean {
        pendingTransactionRepository.deleteAll()
        return true
    }

    // Added for standardized controller support
    override fun findByPendingTransactionId(pendingTransactionId: Long): Optional<PendingTransaction> {
        return pendingTransactionRepository.findByPendingTransactionIdOrderByTransactionDateDesc(pendingTransactionId)
    }

    override fun updatePendingTransaction(pendingTransaction: PendingTransaction): PendingTransaction {
        return pendingTransactionRepository.saveAndFlush(pendingTransaction)
    }
}