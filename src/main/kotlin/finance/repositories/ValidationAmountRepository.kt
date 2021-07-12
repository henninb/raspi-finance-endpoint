package finance.repositories

import finance.domain.TransactionState
import finance.domain.ValidationAmount
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ValidationAmountRepository : JpaRepository<ValidationAmount, Long> {
    fun findByTransactionStateAndAccountId(transactionState: TransactionState, accountId: Long): List<ValidationAmount>
    fun findByAccountId(accountId: Long): List<ValidationAmount>
}