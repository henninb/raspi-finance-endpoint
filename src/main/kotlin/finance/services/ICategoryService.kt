package finance.services

import finance.domain.Category
import java.util.*

interface ICategoryService {
    fun insertCategory(category: Category): Category

    fun findByCategory(categoryName: String): Optional<Category>

    fun deleteByCategoryName(categoryName: String): Boolean

    fun fetchAllActiveCategories(): List<Category>

    fun findByCategoryName(categoryName: String): Optional<Category>
}