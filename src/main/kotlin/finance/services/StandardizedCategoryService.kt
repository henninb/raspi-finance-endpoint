package finance.services

import finance.domain.Category
import finance.domain.ServiceResult
import finance.repositories.CategoryRepository
import finance.repositories.TransactionRepository
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.sql.Timestamp

/**
 * Standardized Category Service implementing ServiceResult pattern
 * Provides both new standardized methods and legacy compatibility
 */
@Service
@Primary
class StandardizedCategoryService(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
) : StandardizedBaseService<Category, Long>() {
    override fun getEntityName(): String = "Category"

    // ===== New Standardized ServiceResult Methods =====

    override fun findAllActive(): ServiceResult<List<Category>> =
        handleServiceOperation("findAllActive", null) {
            val categories = categoryRepository.findByActiveStatusOrderByCategoryName(true)
            categories.map { category ->
                val count = transactionRepository.countByCategoryName(category.categoryName)
                category.categoryCount = count
                category
            }
        }

    override fun findById(id: Long): ServiceResult<Category> =
        handleServiceOperation("findById", id) {
            val optionalCategory = categoryRepository.findByCategoryId(id)
            if (optionalCategory.isPresent) {
                optionalCategory.get()
            } else {
                throw jakarta.persistence.EntityNotFoundException("Category not found: $id")
            }
        }

    override fun save(entity: Category): ServiceResult<Category> =
        handleServiceOperation("save", entity.categoryId) {
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

    override fun update(entity: Category): ServiceResult<Category> =
        handleServiceOperation("update", entity.categoryId) {
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

    override fun deleteById(id: Long): ServiceResult<Boolean> =
        handleServiceOperation("deleteById", id) {
            val optionalCategory = categoryRepository.findByCategoryId(id)
            if (optionalCategory.isEmpty) {
                throw jakarta.persistence.EntityNotFoundException("Category not found: $id")
            }
            categoryRepository.delete(optionalCategory.get())
            true
        }

    fun findByCategoryNameStandardized(categoryName: String): ServiceResult<Category> =
        handleServiceOperation("findByCategoryName", null) {
            val optionalCategory = categoryRepository.findByCategoryName(categoryName)
            if (optionalCategory.isPresent) {
                val category = optionalCategory.get()
                val count = transactionRepository.countByCategoryName(category.categoryName)
                category.categoryCount = count
                category
            } else {
                throw jakarta.persistence.EntityNotFoundException("Category not found: $categoryName")
            }
        }

    fun deleteByCategoryNameStandardized(categoryName: String): ServiceResult<Boolean> =
        handleServiceOperation("deleteByCategoryName", null) {
            val optionalCategory = categoryRepository.findByCategoryName(categoryName)
            if (optionalCategory.isEmpty) {
                throw jakarta.persistence.EntityNotFoundException("Category not found: $categoryName")
            }
            categoryRepository.delete(optionalCategory.get())
            true
        }

    // ===== Business Logic Methods =====

    fun mergeCategories(
        categoryName1: String,
        categoryName2: String,
    ): Category {
        // Find both categories by name
        val category1 =
            categoryRepository.findByCategoryName(categoryName1).orElseThrow {
                RuntimeException("Category $categoryName1 not found")
            }
        val category2 =
            categoryRepository.findByCategoryName(categoryName2).orElseThrow {
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
