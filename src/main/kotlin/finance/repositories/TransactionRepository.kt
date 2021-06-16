package finance.repositories

import finance.domain.Transaction
import finance.domain.TransactionState
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.*

interface TransactionRepository : JpaRepository<Transaction, Long> {
    fun findByGuid(guid: String): Optional<Transaction>

    @Transactional
    fun deleteByGuid(guid: String)


    fun findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc(
        accountNameOwner: String,
        activeStatus: Boolean = true
    ): List<Transaction>

    //need an index by accountNameOwner
    //CREATE INDEX idx_transaction_account_name_owner ON t_transaction(account_name_owner, active_status);
    @Query(value="SELECT COALESCE(A.everything, 0.0) AS everything,  COALESCE(B.cleared, 0.0) AS cleared FROM ( SELECT sum(amount) as everything FROM t_transaction WHERE account_name_owner = :accountNameOwner AND active_status = true) A, ( SELECT SUM(amount) AS cleared FROM t_transaction WHERE account_name_owner = :accountNameOwner AND transaction_state = 'cleared' AND active_status = true) B", nativeQuery=true)
    fun calculateActiveTotalsByAccountNameOwner(
        @Param("accountNameOwner") accountNameOwner: String
    ): List<Any>

    fun findByAccountNameOwnerAndActiveStatusAndTransactionStateNotInOrderByTransactionDateDesc(
        accountNameOwner: String,
        activeStatus: Boolean = true,
        transactionStates: List<TransactionState>
    ): List<Transaction>

    //SELECT account_name_owner, SUM(amount) AS totals_balanced FROM t_transaction
    //fun sumAmountBy
}
