package finance.services

import finance.domain.Category
import finance.repositories.CategoryRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class CategoryService(private var categoryRepository: CategoryRepository) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun insertCategory(category: Category): Boolean {
        categoryRepository.saveAndFlush(category)
        return true
    }

    fun findByCategory(categoryName: String): Optional<Category> {
        val categoryOptional: Optional<Category> = categoryRepository.findByCategory(categoryName)
        if (categoryOptional.isPresent) {
            return categoryOptional
        }
        return Optional.empty()
    }

    fun deleteByCategoryName(categoryName: String) {
        categoryRepository.deleteByCategory(categoryName)
    }

    fun fetchAllCategories(): List<Category> {
        return categoryRepository.findAll()
    }
}