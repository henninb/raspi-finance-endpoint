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

        override fun findAllActive(): ServiceResult<List<Category>> =
            handleServiceOperation("findAllActive", null) {
                val owner = TenantContext.getCurrentOwner()
                val categories = categoryRepository.findByOwnerAndActiveStatusOrderByCategoryName(owner, true)
                applyCategoryCounts(owner, categories)
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

        fun findAllActive(pageable: Pageable): ServiceResult<Page<Category>> =
            handleServiceOperation("findAllActive-paginated", null) {
                val owner = TenantContext.getCurrentOwner()
                val page = categoryRepository.findAllByOwnerAndActiveStatusOrderByCategoryName(owner, true, pageable)
                applyCategoryCounts(owner, page.content)
                page
            }

        private fun applyCategoryCounts(
            owner: String,
            categories: List<Category>,
        ) {
            if (categories.isEmpty()) return
            val countMap =
                transactionRepository
                    .countByOwnerAndCategoryNameIn(owner, categories.map { it.categoryName })
                    .associate { row -> row[0] as String to row[1] as Long }
            categories.forEach { it.categoryCount = countMap[it.categoryName] ?: 0L }
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

        @Transactional
        fun mergeCategories(
            categoryName1: String,
            categoryName2: String,
        ): Category {
            val owner = TenantContext.getCurrentOwner()
            val category1 =
                categoryRepository.findByOwnerAndCategoryName(owner, categoryName1).orElseThrow {
                    RuntimeException("Category $categoryName1 not found")
                }
            val category2 =
                categoryRepository.findByOwnerAndCategoryName(owner, categoryName2).orElseThrow {
                    RuntimeException("Category $categoryName2 not found")
                }

            logger.info("Merging categories: $categoryName2 into $categoryName1")

            val updatedCount = transactionRepository.bulkUpdateCategoryByOwner(owner, categoryName2, categoryName1)
            logger.info("Bulk updated $updatedCount transactions from $categoryName2 to $categoryName1")

            category1.categoryCount += category2.categoryCount
            category2.activeStatus = false

            val mergedCategory = categoryRepository.saveAndFlush(category1)
            logger.info("Successfully merged category $categoryName2 into $categoryName1")

            return mergedCategory
        }
    }
