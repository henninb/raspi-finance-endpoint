package finance.repositories

import finance.domain.Transaction
import finance.domain.TransactionState
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface TransactionRepository : JpaRepository<Transaction, Long> {
    fun findByGuid(guid: String): Optional<Transaction>

    @Transactional
    fun deleteByGuid(guid: String)

    fun findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc(
        accountNameOwner: String,
        activeStatus: Boolean = true
    ): List<Transaction>

    //TODO: 6/27/20201 - add a default param for activeState
    //TODO: 6/27/20201 - what happens if the list is empty
    //TODO: 6/27/20201 - COALESCE, is it required?
    //TODO: 6/24/20201 - CREATE INDEX idx_transaction_account_name_owner ON t_transaction(account_name_owner, active_status);
    @Query(
        value = "SELECT SUM(amount), count(amount), transaction_state FROM t_transaction WHERE account_name_owner = :accountNameOwner AND active_status = true GROUP BY transaction_state",
        nativeQuery = true
    )
    fun sumTotalsForActiveTransactionsByAccountNameOwner(
        @Param("accountNameOwner") accountNameOwner: String
    ): List<Any>

    fun findByAccountNameOwnerAndActiveStatusAndTransactionStateNotInOrderByTransactionDateDesc(
        accountNameOwner: String,
        activeStatus: Boolean = true,
        transactionStates: List<TransactionState>
    ): List<Transaction>
}
