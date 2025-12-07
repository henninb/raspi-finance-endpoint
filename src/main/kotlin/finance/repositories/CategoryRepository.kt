package finance.repositories

import finance.domain.Category
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface CategoryRepository : JpaRepository<Category, Long> {
    fun findByCategoryName(categoryName: String): Optional<Category>

    fun findByCategoryId(categoryId: Long): Optional<Category>

    fun findByActiveStatusOrderByCategoryName(activeStatus: Boolean): List<Category>

    // Paginated query for active categories
    fun findAllByActiveStatusOrderByCategoryName(
        activeStatus: Boolean,
        pageable: Pageable,
    ): Page<Category>
}
