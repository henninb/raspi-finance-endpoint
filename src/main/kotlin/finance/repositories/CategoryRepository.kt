package finance.repositories

import finance.domain.Category
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.*
import javax.transaction.Transactional

interface CategoryRepository : JpaRepository<Category, Long> {

    fun findByCategory(category: String): Optional<Category>

    @Modifying
    @Transactional
    @Query(value = "DELETE from t_category WHERE category = ?1", nativeQuery = true)
    fun deleteByCategory(categoryName: String)
}