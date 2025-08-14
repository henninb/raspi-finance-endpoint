package finance.controllers

import finance.domain.Category
import finance.services.CategoryService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.*

@CrossOrigin
@RestController
@RequestMapping("/category", "/api/category")
class CategoryController(private val categoryService: CategoryService) : BaseController() {

    // curl -k https://localhost:8443/category/select/active
    @GetMapping("/select/active", produces = ["application/json"])
    fun categories(): ResponseEntity<List<Category>> {
        return try {
            logger.debug("Retrieving active categories")
            val categories: List<Category> = categoryService.categories()
            if (categories.isEmpty()) {
                logger.warn("No categories found in the datastore")
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "No categories found")
            }
            logger.info("Retrieved ${categories.size} active categories")
            ResponseEntity.ok(categories)
        } catch (ex: ResponseStatusException) {
            throw ex
        } catch (ex: Exception) {
            logger.error("Failed to retrieve categories: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve categories: ${ex.message}", ex)
        }
    }

    // curl -k https://localhost:8443/category/select/groceries
    @GetMapping("/select/{category_name}")
    fun category(@PathVariable("category_name") categoryName: String): ResponseEntity<Category> {
        return try {
            logger.debug("Retrieving category: $categoryName")
            val category = categoryService.category(categoryName)
                .orElseThrow {
                    logger.warn("Category not found: $categoryName")
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found: $categoryName")
                }
            logger.info("Retrieved category: ${category.categoryName}")
            ResponseEntity.ok(category)
        } catch (ex: ResponseStatusException) {
            throw ex
        } catch (ex: Exception) {
            logger.error("Failed to retrieve category $categoryName: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve category: ${ex.message}", ex)
        }
    }

    // curl -k --header "Content-Type: application/json" --request PUT --data '{"categoryName":"groceries", "activeStatus": true}' https://localhost:8443/category/update/groceries
    @PutMapping("/update/{category_name}", consumes = ["application/json"], produces = ["application/json"])
    fun updateCategory(
        @PathVariable("category_name") categoryName: String,
        @RequestBody toBePatchedCategory: Category
    ): ResponseEntity<Category> {
        return try {
            logger.info("Updating category: $categoryName")
            categoryService.findByCategoryName(categoryName)
                .orElseThrow {
                    logger.warn("Category not found for update: $categoryName")
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found: $categoryName")
                }
            val categoryResponse = categoryService.updateCategory(toBePatchedCategory)
            logger.info("Category updated successfully: $categoryName")
            ResponseEntity.ok(categoryResponse)
        } catch (ex: ResponseStatusException) {
            throw ex
        } catch (ex: Exception) {
            logger.error("Failed to update category $categoryName: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update category: ${ex.message}", ex)
        }
    }

    // curl -k --header "Content-Type: application/json" --request POST --data '{"categoryName":"test", "activeStatus": true}' https://localhost:8443/category/insert
    @PostMapping("/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insertCategory(@RequestBody category: Category): ResponseEntity<Category> {
        return try {
            logger.info("Inserting category: ${category.categoryName}")
            val categoryResponse = categoryService.insertCategory(category)
            logger.info("Category inserted successfully: ${categoryResponse.categoryName}")
            ResponseEntity(categoryResponse, HttpStatus.CREATED)
        } catch (ex: org.springframework.dao.DataIntegrityViolationException) {
            logger.error("Failed to insert category due to data integrity violation: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.CONFLICT, "Duplicate category found.")
        } catch (ex: ResponseStatusException) {
            logger.error("Failed to insert category ${category.categoryName}: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to insert category: ${ex.message}", ex)
        } catch (ex: jakarta.validation.ValidationException) {
            logger.error("Validation error inserting category ${category.categoryName}: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Validation error: ${ex.message}", ex)
        } catch (ex: Exception) {
            logger.error("Unexpected error inserting category ${category.categoryName}: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: ${ex.message}", ex)
        }
    }

    // curl -k --header "Content-Type: application/json" --request DELETE https://localhost:8443/category/delete/test
    @DeleteMapping("/delete/{categoryName}", produces = ["application/json"])
    fun deleteCategory(@PathVariable categoryName: String): ResponseEntity<Category> {
        return try {
            logger.info("Attempting to delete category: $categoryName")
            val categoryToDelete = categoryService.findByCategoryName(categoryName)
                .orElseThrow {
                    logger.warn("Category not found for deletion: $categoryName")
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found: $categoryName")
                }
            
            categoryService.deleteCategory(categoryName)
            logger.info("Category deleted successfully: $categoryName")
            ResponseEntity.ok(categoryToDelete)
        } catch (ex: ResponseStatusException) {
            throw ex
        } catch (ex: Exception) {
            logger.error("Failed to delete category $categoryName: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete category: ${ex.message}", ex)
        }
    }

    // curl -k --header "Content-Type: application/json" --request PUT https://localhost:8443/category/merge?old=categoryA&new=categoryB
    @PutMapping("/merge", produces = ["application/json"])
    fun mergeCategories(
        @RequestParam(value = "new") categoryName1: String,
        @RequestParam("old") categoryName2: String
    ): ResponseEntity<Category> {
        return try {
            logger.info("Merging categories: $categoryName2 into $categoryName1")
            val mergedCategory = categoryService.mergeCategories(categoryName1, categoryName2)
            logger.info("Categories merged successfully: $categoryName2 into $categoryName1")
            ResponseEntity.ok(mergedCategory)
        } catch (ex: Exception) {
            logger.error("Failed to merge categories $categoryName2 into $categoryName1: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to merge categories: ${ex.message}", ex)
        }
    }
}
