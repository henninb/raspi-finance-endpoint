package finance.repositories

import finance.domain.Category
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface CategoryRepository<T : Category> : JpaRepository<T, Long> {

    fun findByCategory(category: String): Optional<Category>
}