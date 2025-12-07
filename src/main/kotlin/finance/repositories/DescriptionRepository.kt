package finance.repositories

import finance.domain.Description
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
// import javax.transaction.Transactional

interface DescriptionRepository : JpaRepository<Description, Long> {
    fun findByActiveStatusOrderByDescriptionName(activeStatus: Boolean): List<Description>

    fun findByDescriptionName(descriptionName: String): Optional<Description>

    fun findByDescriptionId(descriptionId: Long): Optional<Description>

    // Paginated query for active descriptions
    fun findAllByActiveStatusOrderByDescriptionName(
        activeStatus: Boolean,
        pageable: Pageable,
    ): Page<Description>
}
