package finance.repositories

import finance.domain.Description
import org.springframework.data.jpa.repository.JpaRepository
import javax.transaction.Transactional

interface DescriptionRepository : JpaRepository<Description, Long> {
    fun findByActiveStatusOrderByDescription(activeStatus: Boolean): List<Description>

    @Transactional
    fun deleteByDescription(parmName: String)
}