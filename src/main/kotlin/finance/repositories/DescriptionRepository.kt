package finance.repositories

import finance.domain.Description
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*
import javax.transaction.Transactional

interface DescriptionRepository : JpaRepository<Description, Long> {
    fun findByActiveStatusOrderByDescription(activeStatus: Boolean): List<Description>
    fun findByDescription(descriptionName: String): Optional<Description>

//    @Transactional
//    fun deleteByDescription(descriptionName: String)
}