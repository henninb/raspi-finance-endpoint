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
}
