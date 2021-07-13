package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Category

import finance.repositories.CategoryRepository
import io.micrometer.core.annotation.Timed
import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.util.*
import javax.validation.ConstraintViolation
import javax.validation.ValidationException
import javax.validation.Validator

@Service
open class CategoryService(
    private var categoryRepository: CategoryRepository,
    private val validator: Validator,
    private var meterService: MeterService
) : ICategoryService {

    @Timed
    override fun insertCategory(category: Category): Category {
        val constraintViolations: Set<ConstraintViolation<Category>> = validator.validate(category)
        if (constraintViolations.isNotEmpty()) {
            constraintViolations.forEach { constraintViolation -> logger.error(constraintViolation.message) }
            logger.error("Cannot insert category as there is a constraint violation on the data.")
            meterService.incrementExceptionThrownCounter("ValidationException")
            throw ValidationException("Cannot insert category as there is a constraint violation on the data.")
        }
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

    companion object {
        private val mapper = ObjectMapper()
        private val logger = LogManager.getLogger()
    }
}