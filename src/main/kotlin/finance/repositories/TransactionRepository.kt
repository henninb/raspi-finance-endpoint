package finance.repositories

import finance.domain.Transaction
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.*

interface TransactionRepository : JpaRepository<Transaction, Long> {
    //TODO: add LIMIT 1 result
    fun findByGuid(guid: String): Optional<Transaction>

    @Modifying
    @Query("UPDATE TransactionEntity set amount = ?1 WHERE guid = ?2")
    @Transactional
    fun setAmountByGuid(amount: BigDecimal, guild: String)

    @Modifying
    @Query("UPDATE TransactionEntity set cleared = ?1 WHERE guid = ?2")
    @Transactional
    fun setClearedByGuid(cleared: Int, guild: String)


    // Using SpEL expression
    @Query("SELECT SUM(amount) as totalsCleared FROM #{#entityName} WHERE cleared = 1 AND accountNameOwner=?1")
    //@Query(value = "SELECT SUM(amount) AS totals t_transaction WHERE cleared = 1 AND account_name_owner=?1", nativeQuery = true)
    fun getTotalsByAccountNameOwnerCleared(accountNameOwner: String): Double

    // Using SpEL expression
    @Query("SELECT SUM(amount) as totals FROM #{#entityName} WHERE accountNameOwner=?1")
    fun getTotalsByAccountNameOwner(accountNameOwner: String): Double

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM t_transaction WHERE guid = ?1", nativeQuery = true)
    fun deleteByGuid(guid: String)

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM t_transaction_categories WHERE transaction_id = ?1", nativeQuery = true)
    fun deleteByIdFromTransactionCategories(transactionId: Long)

    @Query(value = "SELECT EXTRACT(TIMEZONE FROM now())/3600.0", nativeQuery = true)
    fun selectTimeZoneOffset(): Int

    fun findByAccountNameOwnerIgnoreCaseOrderByTransactionDateDesc(accountNameOwner: String): List<Transaction>


    @Query(value = "SELECT * FROM t_transaction_categories WHERE transaction_id =?", nativeQuery = true)
    fun selectFromTransactionCategories(transactionId: Long): List<Long>
}
