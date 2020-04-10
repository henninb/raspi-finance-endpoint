package finance.services

import finance.domain.Category
import finance.repositories.CategoryRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CategoryService @Autowired constructor(private var categoryRepository: CategoryRepository<Category>) {
    //private val logger = LoggerFactory.getLogger(this.javaClass)

    fun insertCategory(category: Category): Boolean {
        categoryRepository.save(category)
        return true
    }
}