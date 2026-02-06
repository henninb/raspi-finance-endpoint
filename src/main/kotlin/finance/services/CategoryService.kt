package finance.services

import finance.domain.Category
import finance.domain.ServiceResult
import finance.repositories.CategoryRepository
import finance.repositories.TransactionRepository
import finance.utils.TenantContext
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.sql.Timestamp

/**
 * Standardized Category Service implementing ServiceResult pattern
 * Provides both new standardized methods and legacy compatibility
 */
@Service
@Primary
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
) : CrudBaseService<Category, Long>() {
    override fun getEntityName(): String = "Category"

    // ===== New Standardized ServiceResult Methods =====

    override fun findAllActive(): ServiceResult<List<Category>> =
        handleServiceOperation("findAllActive", null) {
            val owner = TenantContext.getCurrentOwner()
            val categories = categoryRepository.findByOwnerAndActiveStatusOrderByCategoryName(owner, true)

            // Batch query to get all counts at once (prevents N+1 query problem)
            val categoryNames = categories.map { it.categoryName }
            val countMap =
                if (categoryNames.isNotEmpty()) {
                    transactionRepository
                        .countByOwnerAndCategoryNameIn(owner, categoryNames)
                        .associate { row -> row[0] as String to row[1] as Long }
                } else {
                    emptyMap()
                }

            // Apply counts to categories
            categories.forEach { category ->
                category.categoryCount = countMap[category.categoryName] ?: 0L
            }

            categories
        }

    override fun findById(id: Long): ServiceResult<Category> =
        handleServiceOperation("findById", id) {
            val owner = TenantContext.getCurrentOwner()
            val optionalCategory = categoryRepository.findByOwnerAndCategoryId(owner, id)
            if (optionalCategory.isPresent) {
                optionalCategory.get()
            } else {
                throw jakarta.persistence.EntityNotFoundException("Category not found: $id")
            }
        }

    override fun save(entity: Category): ServiceResult<Category> =
        handleServiceOperation("save", entity.categoryId) {
            val owner = TenantContext.getCurrentOwner()
            entity.owner = owner

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
            val owner = TenantContext.getCurrentOwner()
            val existingCategory = categoryRepository.findByOwnerAndCategoryId(owner, entity.categoryId)
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
            val owner = TenantContext.getCurrentOwner()
            val optionalCategory = categoryRepository.findByOwnerAndCategoryId(owner, id)
            if (optionalCategory.isEmpty) {
                throw jakarta.persistence.EntityNotFoundException("Category not found: $id")
            }
            categoryRepository.delete(optionalCategory.get())
            true
        }

    // ===== Paginated ServiceResult Methods =====

    /**
     * Find all active categories with pagination.
     * Sorted by categoryName ascending. Preserves transaction count batch loading.
     */
    fun findAllActive(pageable: Pageable): ServiceResult<Page<Category>> =
        handleServiceOperation("findAllActive-paginated", null) {
            val owner = TenantContext.getCurrentOwner()
            val page = categoryRepository.findAllByOwnerAndActiveStatusOrderByCategoryName(owner, true, pageable)

            // Batch query to get all counts at once (prevents N+1 query problem)
            val categoryNames = page.content.map { it.categoryName }
            val countMap =
                if (categoryNames.isNotEmpty()) {
                    transactionRepository
                        .countByOwnerAndCategoryNameIn(owner, categoryNames)
                        .associate { row -> row[0] as String to row[1] as Long }
                } else {
                    emptyMap()
                }

            // Apply counts to categories
            page.content.forEach { category ->
                category.categoryCount = countMap[category.categoryName] ?: 0L
            }

            page
        }

    fun findByCategoryNameStandardized(categoryName: String): ServiceResult<Category> =
        handleServiceOperation("findByCategoryName", null) {
            val owner = TenantContext.getCurrentOwner()
            val optionalCategory = categoryRepository.findByOwnerAndCategoryName(owner, categoryName)
            if (optionalCategory.isPresent) {
                val category = optionalCategory.get()
                val count = transactionRepository.countByOwnerAndCategoryName(owner, category.categoryName)
                category.categoryCount = count
                category
            } else {
                throw jakarta.persistence.EntityNotFoundException("Category not found: $categoryName")
            }
        }

    fun deleteByCategoryNameStandardized(categoryName: String): ServiceResult<Boolean> =
        handleServiceOperation("deleteByCategoryName", null) {
            val owner = TenantContext.getCurrentOwner()
            val optionalCategory = categoryRepository.findByOwnerAndCategoryName(owner, categoryName)
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
        val owner = TenantContext.getCurrentOwner()
        // Find both categories by name
        val category1 =
            categoryRepository.findByOwnerAndCategoryName(owner, categoryName1).orElseThrow {
                RuntimeException("Category $categoryName1 not found")
            }
        val category2 =
            categoryRepository.findByOwnerAndCategoryName(owner, categoryName2).orElseThrow {
                RuntimeException("Category $categoryName2 not found")
            }

        logger.info("Merging categories: $categoryName2 into $categoryName1")

        // Reassign transactions from category2 to category1
        val transactionsToUpdate = transactionRepository.findByOwnerAndCategoryAndActiveStatusOrderByTransactionDateDesc(owner, categoryName2, true)
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
