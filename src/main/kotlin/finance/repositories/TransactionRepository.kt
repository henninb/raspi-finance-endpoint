package finance.repositories

import finance.domain.Transaction
import finance.domain.TransactionState
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface TransactionRepository : JpaRepository<Transaction, Long> {
    fun findByGuid(guid: String): Optional<Transaction>

    @Transactional
    fun deleteByGuid(guid: String)

    fun findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc(accountNameOwner: String, activeStatus: Boolean = true): List<Transaction>
    fun findByAccountNameOwnerAndActiveStatusAndTransactionStateNotInOrderByTransactionDateDesc(accountNameOwner: String, activeStatus: Boolean = true, transactionStates: List<TransactionState>) : List<Transaction>

    //SELECT account_name_owner, SUM(amount) AS totals_balanced FROM t_transaction
    //fun sumAmountBy
}
