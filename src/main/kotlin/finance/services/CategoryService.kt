package finance.services

import finance.domain.Category
import finance.repositories.CategoryRepository
import io.micrometer.core.annotation.Timed
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.util.*
import javax.validation.ConstraintViolation

@Service
open class CategoryService(
    private var categoryRepository: CategoryRepository
) : ICategoryService, BaseService() {

    @Timed
    override fun insertCategory(category: Category): Category {
        val constraintViolations: Set<ConstraintViolation<Category>> = validator.validate(category)
        handleConstraintViolations(constraintViolations, meterService)
        category.dateAdded = Timestamp(Calendar.getInstance().time.time)
        category.dateUpdated = Timestamp(Calendar.getInstance().time.time)
        return categoryRepository.saveAndFlush(category)
    }

    @Timed
    override fun findByCategory(categoryName: String): Optional<Category> {
        val categoryOptional: Optional<Category> = categoryRepository.findByCategory(categoryName)
        if (categoryOptional.isPresent) {
            return categoryOptional
        }
        return Optional.empty()
    }

    @Timed
    override fun deleteByCategoryName(categoryName: String): Boolean {
        categoryRepository.deleteByCategory(categoryName)
        return true
    }

    @Timed
    override fun fetchAllActiveCategories(): List<Category> {
        return categoryRepository.findByActiveStatusOrderByCategory(true)
    }

    @Timed
    override fun findByCategoryName(categoryName: String): Optional<Category> {
        return categoryRepository.findByCategory(categoryName)
    }
}