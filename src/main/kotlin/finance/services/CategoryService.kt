package finance.services

import finance.domain.Category
import finance.repositories.CategoryRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CategoryService {
    //private val logger = LoggerFactory.getLogger(this.javaClass)

    @Autowired
    lateinit private var categoryRepository: CategoryRepository<Category>

    fun insertCategory(category: Category): Boolean {
        categoryRepository.save(category)
        return true
    }
}