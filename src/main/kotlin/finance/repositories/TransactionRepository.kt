package finance.repositories

import finance.domain.Transaction
import finance.domain.TransactionState
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.*

interface TransactionRepository : JpaRepository<Transaction, Long> {

    //fun findTOPByGuid(guid: String): Optional<Transaction>
    fun findByGuid(guid: String): Optional<Transaction>

    @Modifying
    @Query("UPDATE TransactionEntity set amount = ?1 WHERE guid = ?2")
    @Transactional
    fun setAmountByGuid(amount: BigDecimal, guild: String)

    @Modifying
    @Query("UPDATE TransactionEntity set transaction_state = ?1 WHERE guid = ?2")
    @Transactional
    fun setTransactionStateByGuid(transactionState: TransactionState, guild: String)

    //@Transactional
    // Using SpEL expression
    @Query("SELECT COALESCE(SUM(amount), 0.0) as totalsCleared FROM #{#entityName} WHERE transactionState = 'cleared' AND accountNameOwner=?1")
    //@Query(value = "SELECT SUM(amount) AS totals t_transaction WHERE cleared = 1 AND account_name_owner=?1", nativeQuery = true)
    fun getTotalsByAccountNameOwnerTransactionState(accountNameOwner: String): Double

    //@Transactional
    // Using SpEL expression
    @Query("SELECT COALESCE(SUM(amount), 0.0) as totals FROM #{#entityName} WHERE accountNameOwner=?1")
    fun getTotalsByAccountNameOwner(accountNameOwner: String): Double

    //@Modifying
    @Transactional
    //@Query(value = "DELETE FROM t_transaction WHERE guid = ?1", nativeQuery = true)
    fun deleteByGuid(guid: String)

    fun findByAccountNameOwnerIgnoreCaseOrderByTransactionDateDesc(accountNameOwner: String): List<Transaction>

    @Query(value = "SELECT * FROM t_transaction_categories WHERE transaction_id =?", nativeQuery = true)
    fun selectFromTransactionCategories(transactionId: Long): List<Long>
}
