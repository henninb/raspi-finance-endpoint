package finance.services

import finance.domain.Category
import finance.domain.ServiceResult
import finance.repositories.CategoryRepository
import finance.repositories.TransactionRepository
import jakarta.validation.ValidationException
import jakarta.validation.Validator
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.util.*

/**
 * Standardized Category Service implementing ServiceResult pattern
 * Provides both new standardized methods and legacy compatibility
 */
@Service
class StandardizedCategoryService(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) : StandardizedBaseService<Category, Long>(), ICategoryService {

    override fun getEntityName(): String = "Category"

    // ===== New Standardized ServiceResult Methods =====

    override fun findAllActive(): ServiceResult<List<Category>> {
        return handleServiceOperation("findAllActive", null) {
            val categories = categoryRepository.findByActiveStatusOrderByCategoryName(true)
            categories.map { category ->
                val count = transactionRepository.countByCategoryName(category.categoryName)
                category.categoryCount = count
                category
            }
        }
    }

    override fun findById(id: Long): ServiceResult<Category> {
        return handleServiceOperation("findById", id) {
            val optionalCategory = categoryRepository.findByCategoryId(id)
            if (optionalCategory.isPresent) {
                optionalCategory.get()
            } else {
                throw jakarta.persistence.EntityNotFoundException("Category not found: $id")
            }
        }
    }

    override fun save(entity: Category): ServiceResult<Category> {
        return handleServiceOperation("save", entity.categoryId) {
            val violations = validator.validate(entity)
            if (violations.isNotEmpty()) {
                throw jakarta.validation.ConstraintViolationException("Validation failed", violations)
            }

            // Set timestamps
            val timestamp = Timestamp(System.currentTimeMillis())
            entity.dateAdded = timestamp
            entity.dateUpdated = timestamp

            categoryRepository.saveAndFlush(entity)
        }
    }

    override fun update(entity: Category): ServiceResult<Category> {
        return handleServiceOperation("update", entity.categoryId) {
            val existingCategory = categoryRepository.findByCategoryId(entity.categoryId!!)
            if (existingCategory.isEmpty) {
                throw jakarta.persistence.EntityNotFoundException("Category not found: ${entity.categoryId}")
            }

            // Update fields from the provided entity
            val categoryToUpdate = existingCategory.get()
            categoryToUpdate.categoryName = entity.categoryName
            categoryToUpdate.activeStatus = entity.activeStatus
            categoryToUpdate.dateUpdated = Timestamp(System.currentTimeMillis())

            categoryRepository.saveAndFlush(categoryToUpdate)
        }
    }

    override fun deleteById(id: Long): ServiceResult<Boolean> {
        return handleServiceOperation("deleteById", id) {
            val optionalCategory = categoryRepository.findByCategoryId(id)
            if (optionalCategory.isEmpty) {
                throw jakarta.persistence.EntityNotFoundException("Category not found: $id")
            }
            categoryRepository.delete(optionalCategory.get())
            true
        }
    }

    // ===== Legacy Method Compatibility =====

    override fun categories(): List<Category> {
        val result = findAllActive()
        return when (result) {
            is ServiceResult.Success -> result.data
            else -> emptyList()
        }
    }

    override fun insertCategory(category: Category): Category {
        val result = save(category)
        return when (result) {
            is ServiceResult.Success -> result.data
            is ServiceResult.ValidationError -> {
                val violations = result.errors.map { (field, message) ->
                    object : jakarta.validation.ConstraintViolation<Category> {
                        override fun getMessage(): String = message
                        override fun getMessageTemplate(): String = message
                        override fun getRootBean(): Category = category
                        override fun getRootBeanClass(): Class<Category> = Category::class.java
                        override fun getLeafBean(): Any = category
                        override fun getExecutableParameters(): Array<Any> = emptyArray()
                        override fun getExecutableReturnValue(): Any? = null
                        override fun getPropertyPath(): jakarta.validation.Path {
                            return object : jakarta.validation.Path {
                                override fun toString(): String = field
                                override fun iterator(): MutableIterator<jakarta.validation.Path.Node> = mutableListOf<jakarta.validation.Path.Node>().iterator()
                            }
                        }
                        override fun getInvalidValue(): Any? = null
                        override fun getConstraintDescriptor(): jakarta.validation.metadata.ConstraintDescriptor<*>? = null
                        override fun <U : Any?> unwrap(type: Class<U>?): U = throw UnsupportedOperationException()
                    }
                }.toSet()
                throw ValidationException(jakarta.validation.ConstraintViolationException("Validation failed", violations))
            }
            else -> throw RuntimeException("Failed to insert category: ${result}")
        }
    }

    override fun updateCategory(category: Category): Category {
        val result = update(category)
        return when (result) {
            is ServiceResult.Success -> result.data
            is ServiceResult.NotFound -> throw RuntimeException("Category not updated as the category does not exist: ${category.categoryId}.")
            else -> throw RuntimeException("Failed to update category: ${result}")
        }
    }

    override fun findByCategoryName(categoryName: String): Optional<Category> {
        return categoryRepository.findByCategoryName(categoryName)
    }

    override fun category(categoryName: String): Optional<Category> {
        return findByCategoryName(categoryName)
    }

    override fun deleteCategory(categoryName: String): Boolean {
        val optionalCategory = categoryRepository.findByCategoryName(categoryName)
        if (optionalCategory.isEmpty) {
            return false
        }
        categoryRepository.delete(optionalCategory.get())
        return true
    }

    override fun mergeCategories(categoryName1: String, categoryName2: String): Category {
        // Find both categories by name
        val category1 = categoryRepository.findByCategoryName(categoryName1).orElseThrow {
            RuntimeException("Category $categoryName1 not found")
        }
        val category2 = categoryRepository.findByCategoryName(categoryName2).orElseThrow {
            RuntimeException("Category $categoryName2 not found")
        }

        logger.info("Merging categories: $categoryName2 into $categoryName1")

        // Reassign transactions from category2 to category1
        val transactionsToUpdate = transactionRepository.findByCategoryAndActiveStatusOrderByTransactionDateDesc(categoryName2)
        logger.info("Found ${transactionsToUpdate.size} transactions to reassign from $categoryName2 to $categoryName1")

        transactionsToUpdate.forEach { transaction ->
            transaction.category = categoryName1
            transactionRepository.saveAndFlush(transaction)
        }

        // Merge category counts
        category1.categoryCount += category2.categoryCount

        // Mark category2 as inactive
        category2.activeStatus = false

        // Save the updated category1
        val mergedCategory = categoryRepository.saveAndFlush(category1)
        logger.info("Successfully merged category $categoryName2 into $categoryName1")

        return mergedCategory
    }
}