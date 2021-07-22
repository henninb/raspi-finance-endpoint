package finance.repositories

import finance.domain.Category
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*
import javax.transaction.Transactional

interface CategoryRepository : JpaRepository<Category, Long> {

    fun findByCategory(categoryName: String): Optional<Category>

    fun findByActiveStatusOrderByCategory(activeStatus: Boolean): List<Category>

//    @Transactional
//    fun deleteByCategory(categoryName: String)
}