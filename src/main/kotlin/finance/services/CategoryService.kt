package finance.services

import finance.domain.Category
import finance.repositories.CategoryRepository
import finance.repositories.TransactionRepository
import io.micrometer.core.annotation.Timed
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.util.*
import jakarta.validation.ConstraintViolation

@Service
open class CategoryService(
    private var categoryRepository: CategoryRepository,
    private var transactionRepository: TransactionRepository
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
    override fun category(categoryName: String): Optional<Category> {
        val categoryOptional: Optional<Category> = categoryRepository.findByCategoryName(categoryName)
        if (categoryOptional.isPresent) {
            return categoryOptional
        }
        return Optional.empty()
    }

    @Timed
    override fun deleteCategory(categoryName: String): Boolean {
        val category = categoryRepository.findByCategoryName(categoryName).get()
        categoryRepository.delete(category)
        return true
    }

    @Timed
    override fun categories(): List<Category> {
        val categories = categoryRepository.findByActiveStatusOrderByCategoryName(true)
        return categories.map { category ->
            val count = transactionRepository.countByCategoryName(category.categoryName)
            category.categoryCount = count
            category
        }
    }

    @Timed
    override fun findByCategoryName(categoryName: String): Optional<Category> {
        return categoryRepository.findByCategoryName(categoryName)
    }

    @Transactional
    @Timed
    override fun updateCategory(category: Category): Category {
        val optionalCategory = categoryRepository.findByCategoryId(category.categoryId)

        if (optionalCategory.isPresent) {
            val categoryToUpdate = optionalCategory.get()

            // Updating fields
            categoryToUpdate.categoryName = category.categoryName
            categoryToUpdate.activeStatus = category.activeStatus
            categoryToUpdate.dateUpdated = Timestamp(Calendar.getInstance().time.time)
            logger.info("category update")
            return categoryRepository.saveAndFlush(categoryToUpdate)
        }

        throw RuntimeException("Category not updated as the category does not exist: ${category.categoryId}.")
    }

    @Transactional
    @Timed
    override fun mergeCategories(categoryName1: String, categoryName2: String): Category {
        // Find both categories by name
        val category1 = categoryRepository.findByCategoryName(categoryName1).orElseThrow {
            RuntimeException("Category $categoryName1 not found")
        }
        val category2 = categoryRepository.findByCategoryName(categoryName2).orElseThrow {
            RuntimeException("Category $categoryName2 not found")
        }

        // Reassign transactions from category2 to category1
        val transactionsToUpdate = transactionRepository.findByCategoryAndActiveStatusOrderByTransactionDateDesc(categoryName2)
        transactionsToUpdate.forEach { transaction ->
            transaction.category = categoryName1
            transactionRepository.saveAndFlush(transaction)
        }

        // Optionally, merge other attributes (e.g., category counts, descriptions)
        // You can decide if you want to keep category1's name or category2's
        category1.categoryCount += category2.categoryCount // You might want to combine counts if needed

        // Mark category2 as inactive or delete it
        category2.activeStatus = false // You could also delete it if required: categoryRepository.delete(category2)

        // Save the updated category1
        categoryRepository.saveAndFlush(category1)

        // Return the merged category (category1 in this case)
        return category1
    }

}