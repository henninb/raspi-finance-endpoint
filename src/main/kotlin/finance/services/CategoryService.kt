package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Category
import finance.repositories.CategoryRepository
import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Service
import java.util.*
import javax.validation.ConstraintViolation
import javax.validation.ValidationException
import javax.validation.Validator

@Service
class CategoryService(private var categoryRepository: CategoryRepository, private val validator: Validator) {

    fun insertCategory(category: Category): Boolean {
        val constraintViolations: Set<ConstraintViolation<Category>> = validator.validate(category)
        if (constraintViolations.isNotEmpty()) {
            logger.error("Cannot insert category as there is a constraint violation on the data.")
            throw ValidationException("Cannot insert category as there is a constraint violation on the data.")
        }

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

    companion object {
        private val mapper = ObjectMapper()
        private val logger = LogManager.getLogger()
    }
}