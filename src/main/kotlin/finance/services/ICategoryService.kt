package finance.services

import finance.domain.Category
import java.util.*

interface ICategoryService {
    fun insertCategory(category: Category): Category

    fun category(categoryName: String): Optional<Category>

    fun deleteCategory(categoryName: String): Boolean

    fun categories(): List<Category>

    fun findByCategoryName(categoryName: String): Optional<Category>
}