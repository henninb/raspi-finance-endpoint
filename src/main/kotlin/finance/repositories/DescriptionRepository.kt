package finance.repositories

import finance.domain.Description
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*
//import javax.transaction.Transactional

interface DescriptionRepository : JpaRepository<Description, Long> {
    fun findByActiveStatusOrderByDescriptionName(activeStatus: Boolean): List<Description>
    fun findByDescriptionName(descriptionName: String): Optional<Description>
    fun findByDescriptionId(descriptionId: Long): Optional<Description>
}