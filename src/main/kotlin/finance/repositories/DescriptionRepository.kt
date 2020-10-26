package finance.repositories

import finance.domain.Description
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*
import javax.transaction.Transactional

interface DescriptionRepository : JpaRepository<Description, Long> {
    fun findByDescription(parmName: String): Optional<Description>

    @Transactional
    fun deleteByDescription(parmName: String)
}