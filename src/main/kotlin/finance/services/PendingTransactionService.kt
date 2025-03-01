package finance.services

import finance.domain.PendingTransaction
import finance.repositories.PendingTransactionRepository
import org.springframework.stereotype.Service




@Service
open class PendingTransactionService(
        private var pendingTransactionRepository: PendingTransactionRepository,
    ) : IPendingTransactionService, BaseService() {


    override fun insertPendingTransaction(pendingTransaction: PendingTransaction): PendingTransaction {
        return pendingTransaction

    }

    override fun deletePendingTransaction(pendingTransactionId: Long): Boolean {
        return true
    }

    override fun getAllPendingTransactions(): List<PendingTransaction> {
        return emptyList()


    }

}