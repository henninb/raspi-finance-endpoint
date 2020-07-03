package finance.services

import finance.domain.Category
import finance.repositories.CategoryRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
class CategoryService (private var categoryRepository: CategoryRepository) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun insertCategory(category: Category): Boolean {
        categoryRepository.save(category)
        return true
    }

    fun deleteByCategory(categoryName: String) {
        logger.info("deleteByCategory")
        categoryRepository.deleteByCategory(categoryName)
    }

    fun findByCategory(categoryName: String): Optional<Category> {
        val categoryOptional: Optional<Category> = categoryRepository.findByCategory(categoryName)
        if (categoryOptional.isPresent) {
            return categoryOptional
        }
        return Optional.empty()
    }

}