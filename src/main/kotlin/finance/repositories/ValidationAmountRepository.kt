package finance.repositories

import finance.domain.ValidationAmount
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ValidationAmountRepository : JpaRepository<ValidationAmount, Long> {
    fun findByAccountId(accountId: Long): List<ValidationAmount>
}