package finance.repositories

import finance.domain.ValidationAmount
import org.springframework.data.jpa.repository.JpaRepository

interface ValidationAmountRepository : JpaRepository<ValidationAmount, Long> {
}