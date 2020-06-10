package finance.services

import finance.domain.Category
import finance.repositories.CategoryRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
open class CategoryService @Autowired constructor(private var categoryRepository: CategoryRepository<Category>) {
    //private val logger = LoggerFactory.getLogger(this.javaClass)

    fun insertCategory(category: Category): Boolean {
        categoryRepository.save(category)
        return true
    }

    fun findByCategory(categoryName: String ): Optional<Category> {
        val categoryOptional: Optional<Category> = categoryRepository.findByCategory(categoryName)
        if( categoryOptional.isPresent ) {
            return categoryOptional
        }
        return Optional.empty()
    }

    fun deleteByCategory(categoryName: String) {
        categoryRepository.deleteByCategory(categoryName)
    }
}