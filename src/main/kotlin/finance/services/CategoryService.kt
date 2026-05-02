package finance.services

import finance.configurations.ResilienceComponents
import finance.domain.Category
import finance.domain.ServiceResult
import finance.repositories.CategoryRepository
import finance.repositories.TransactionRepository
import finance.utils.TenantContext
import finance.utils.orThrowNotFound
import jakarta.validation.Validator
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CategoryService
    constructor(
        private val categoryRepository: CategoryRepository,
        private val transactionRepository: TransactionRepository,
        meterService: MeterService,
        validator: Validator,
        resilienceComponents: ResilienceComponents,
    ) : CrudBaseService<Category, Long>(meterService, validator, resilienceComponents) {
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
                categoryRepository.findByOwnerAndCategoryId(owner, id).orThrowNotFound("Category", id)
            }

        override fun save(entity: Category): ServiceResult<Category> =
            handleServiceOperation("save", entity.categoryId) {
                val owner = TenantContext.getCurrentOwner()
                entity.owner = owner
                validateOrThrow(entity)
                val timestamp = nowTimestamp()
                entity.dateAdded = timestamp
                entity.dateUpdated = timestamp
                categoryRepository.saveAndFlush(entity)
            }

        override fun update(entity: Category): ServiceResult<Category> =
            handleServiceOperation("update", entity.categoryId) {
                val owner = TenantContext.getCurrentOwner()
                val categoryToUpdate =
                    categoryRepository
                        .findByOwnerAndCategoryId(owner, entity.categoryId)
                        .orThrowNotFound("Category", entity.categoryId)
                categoryToUpdate.categoryName = entity.categoryName
                categoryToUpdate.activeStatus = entity.activeStatus
                categoryToUpdate.dateUpdated = nowTimestamp()
                categoryRepository.saveAndFlush(categoryToUpdate)
            }

        override fun deleteById(id: Long): ServiceResult<Category> =
            handleServiceOperation("deleteById", id) {
                val owner = TenantContext.getCurrentOwner()
                val category = categoryRepository.findByOwnerAndCategoryId(owner, id).orThrowNotFound("Category", id)
                categoryRepository.delete(category)
                category
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
                val category =
                    categoryRepository
                        .findByOwnerAndCategoryName(owner, categoryName)
                        .orThrowNotFound("Category", categoryName)
                category.categoryCount = transactionRepository.countByOwnerAndCategoryName(owner, category.categoryName)
                category
            }

        fun deleteByCategoryNameStandardized(categoryName: String): ServiceResult<Category> =
            handleServiceOperation("deleteByCategoryName", null) {
                val owner = TenantContext.getCurrentOwner()
                val category =
                    categoryRepository
                        .findByOwnerAndCategoryName(owner, categoryName)
                        .orThrowNotFound("Category", categoryName)
                categoryRepository.delete(category)
                category
            }

        // ===== Business Logic Methods =====

        @Transactional
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

            // Reassign transactions from category2 to category1 via single bulk UPDATE
            val updatedCount = transactionRepository.bulkUpdateCategoryByOwner(owner, categoryName2, categoryName1)
            logger.info("Bulk updated $updatedCount transactions from $categoryName2 to $categoryName1")

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
