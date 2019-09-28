package finance.repositories

import finance.models.Transaction
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface TransactionRepository<T : Transaction> : JpaRepository<T, Long> {
    override fun findAll(pageable: Pageable): Page<T>

    fun findByAccountNameOwnerIgnoreCaseOrderByTransactionDateDesc(accountNameOwner: String): List<Transaction>
    fun findByAccountNameOwnerIgnoreCaseOrderByTransactionDate(pageable : Pageable, accountNameOwner: String) : Page<Transaction>

    //TODO: add LIMIT 1 result
    fun findByGuid(guid: String): Optional<Transaction>

    // Using SpEL expression
    @Query("SELECT SUM(amount) as totalsCleared FROM #{#entityName} WHERE cleared = 1 AND accountNameOwner=?1")
    //@Query(value = "SELECT SUM(amount) AS totals t_transaction WHERE cleared = 1 AND account_name_owner=?1", nativeQuery = true)
    fun getTotalsByAccountNameOwnerCleared(accountNameOwner: String): Double

    // Using SpEL expression
    @Query("SELECT SUM(amount) as totals FROM #{#entityName} WHERE accountNameOwner=?1")
    fun getTotalsByAccountNameOwner(accountNameOwner: String): Double

    @Modifying
    @Transactional
    @Query(value = "DELETE from t_transaction WHERE guid = ?1", nativeQuery = true)
    fun deleteByGuid(guid: String)

    @Query(value="SELECT EXTRACT(TIMEZONE FROM now())/3600.0" , nativeQuery = true)
    fun selectTimeZoneOffset(): Int
}
