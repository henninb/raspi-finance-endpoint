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

    //TODO: should this be switched to fun findTOPByGuid(guid: String): Optional<Transaction>?
    fun findByGuid(guid: String): Optional<Transaction>

    // Using SpEL expression
    @Query("SELECT COALESCE(SUM(amount), 0.0) as totalsCleared FROM #{#entityName} WHERE transactionState = 'cleared' AND accountNameOwner=?1")
    //@Query(value = "SELECT SUM(amount) AS totals t_transaction WHERE cleared = 1 AND account_name_owner=?1", nativeQuery = true)
    fun getTotalsByAccountNameOwnerTransactionState(accountNameOwner: String): Double

    // Using SpEL expression
    @Query("SELECT COALESCE(SUM(amount), 0.0) as totals FROM #{#entityName} WHERE accountNameOwner=?1")
    fun getTotalsByAccountNameOwner(accountNameOwner: String): Double

    @Transactional
    fun deleteByGuid(guid: String)

    fun findByAccountNameOwnerIgnoreCaseOrderByTransactionDateDesc(accountNameOwner: String): List<Transaction>

//    @Query(value = "SELECT * FROM t_transaction_categories WHERE transaction_id =?", nativeQuery = true)
//    fun selectFromTransactionCategories(transactionId: Long): List<Long>
}
