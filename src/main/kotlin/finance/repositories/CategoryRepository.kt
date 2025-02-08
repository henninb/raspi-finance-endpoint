package finance.repositories

import finance.domain.Category
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface CategoryRepository : JpaRepository<Category, Long> {

    fun findByCategoryName(categoryName: String): Optional<Category>
    fun findByCategoryId(categoryId: Long): Optional<Category>
    fun findByActiveStatusOrderByCategoryName(activeStatus: Boolean): List<Category>
}