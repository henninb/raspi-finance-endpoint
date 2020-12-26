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

    fun findByGuid(guid: String): Optional<Transaction>

    @Transactional
    fun deleteByGuid(guid: String)

    fun findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc(accountNameOwner: String, activeStatus: Boolean = true): List<Transaction>

    @Query(value = "SELECT account_name_owner FROM t_transaction WHERE transaction_state = 'cleared' and account_name_owner in (select account_name_owner from t_account where account_type = 'credit' and active_status = true) or (transaction_state = 'outstanding' and account_type = 'credit' and description ='payment') group by account_name_owner having sum(amount) > 0 order by account_name_owner", nativeQuery = true)
    fun findAccountsThatRequirePayment(): List<String>
}
