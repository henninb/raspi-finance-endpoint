package finance.controllers

import finance.domain.Category
import finance.services.ICategoryService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import jakarta.validation.Valid
import java.util.*

@CrossOrigin
@RestController
@RequestMapping("/api/category")
class CategoryController(private val categoryService: ICategoryService) :
    StandardizedBaseController(), StandardRestController<Category, String> {

    // ===== STANDARDIZED ENDPOINTS (NEW) =====

    /**
     * Standardized collection retrieval - GET /api/category/active
     * Returns empty list instead of throwing 404 (standardized behavior)
     */
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<Category>> {
        return handleCrudOperation("Find all active categories", null) {
            logger.debug("Retrieving all active categories")
            val categories: List<Category> = categoryService.categories()
            logger.info("Retrieved ${categories.size} active categories")
            categories
        }
    }

    /**
     * Standardized single entity retrieval - GET /api/category/{categoryName}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @GetMapping("/{categoryName}", produces = ["application/json"])
    override fun findById(@PathVariable categoryName: String): ResponseEntity<Category> {
        return handleCrudOperation("Find category by name", categoryName) {
            logger.debug("Retrieving category: $categoryName")
            val category = categoryService.findByCategoryName(categoryName)
                .orElseThrow {
                    logger.warn("Category not found: $categoryName")
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found: $categoryName")
                }
            logger.info("Retrieved category: $categoryName")
            category
        }
    }

    /**
     * Standardized entity creation - POST /api/category
     * Returns 201 CREATED
     */
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    override fun save(@Valid @RequestBody category: Category): ResponseEntity<Category> {
        return handleCreateOperation("Category", category.categoryName) {
            logger.info("Creating category: ${category.categoryName}")
            val result = categoryService.insertCategory(category)
            logger.info("Category created successfully: ${category.categoryName}")
            result
        }
    }

    /**
     * Standardized entity update - PUT /api/category/{categoryName}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @PutMapping("/{categoryName}", consumes = ["application/json"], produces = ["application/json"])
    override fun update(@PathVariable categoryName: String, @Valid @RequestBody category: Category): ResponseEntity<Category> {
        return handleCrudOperation("Update category", categoryName) {
            logger.info("Updating category: $categoryName")
            // Validate category exists first
            categoryService.findByCategoryName(categoryName)
                .orElseThrow {
                    logger.warn("Category not found for update: $categoryName")
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found: $categoryName")
                }
            val result = categoryService.updateCategory(category)
            logger.info("Category updated successfully: $categoryName")
            result
        }
    }

    /**
     * Standardized entity deletion - DELETE /api/category/{categoryName}
     * Returns 200 OK with deleted entity
     */
    @DeleteMapping("/{categoryName}", produces = ["application/json"])
    override fun deleteById(@PathVariable categoryName: String): ResponseEntity<Category> {
        return handleDeleteOperation(
            "Category",
            categoryName,
            { categoryService.findByCategoryName(categoryName) },
            { categoryService.deleteCategory(categoryName) }
        )
    }

    // ===== LEGACY ENDPOINTS (BACKWARD COMPATIBILITY) =====

    /**
     * Legacy endpoint - GET /api/category/select/active
     * Maintains original behavior: throws 404 if empty
     */
    @GetMapping("/select/active", produces = ["application/json"])
    fun categories(): ResponseEntity<List<Category>> {
        return try {
            logger.debug("Retrieving active categories (legacy endpoint)")
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

    /**
     * Legacy endpoint - GET /api/category/select/{category_name}
     * Maintains snake_case @PathVariable annotation for backward compatibility
     */
    @GetMapping("/select/{category_name}")
    fun category(@PathVariable("category_name") categoryName: String): ResponseEntity<Category> {
        return try {
            logger.debug("Retrieving category: $categoryName (legacy endpoint)")
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

    /**
     * Legacy endpoint - POST /api/category/insert
     */
    @PostMapping("/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insertCategory(@RequestBody category: Category): ResponseEntity<Category> {
        return try {
            logger.info("Inserting category: ${category.categoryName} (legacy endpoint)")
            val categoryResponse = categoryService.insertCategory(category)
            logger.info("Category inserted successfully: ${categoryResponse.categoryName}")
            ResponseEntity(categoryResponse, HttpStatus.CREATED)
        } catch (ex: org.springframework.dao.DataIntegrityViolationException) {
            logger.error("Failed to insert category due to data integrity violation: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.CONFLICT, "Duplicate category found.")
        } catch (ex: jakarta.validation.ValidationException) {
            logger.error("Validation error inserting category ${category.categoryName}: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Validation error: ${ex.message}", ex)
        } catch (ex: IllegalArgumentException) {
            logger.error("Invalid input inserting category ${category.categoryName}: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input: ${ex.message}", ex)
        } catch (ex: Exception) {
            logger.error("Unexpected error inserting category ${category.categoryName}: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: ${ex.message}", ex)
        }
    }

    /**
     * Legacy endpoint - PUT /api/category/update/{category_name}
     * Maintains snake_case @PathVariable annotation for backward compatibility
     */
    @PutMapping("/update/{category_name}", consumes = ["application/json"], produces = ["application/json"])
    fun updateCategory(
        @PathVariable("category_name") categoryName: String,
        @RequestBody toBePatchedCategory: Category
    ): ResponseEntity<Category> {
        return try {
            logger.info("Updating category: $categoryName (legacy endpoint)")
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

    /**
     * Legacy endpoint - DELETE /api/category/delete/{categoryName}
     */
    @DeleteMapping("/delete/{categoryName}", produces = ["application/json"])
    fun deleteCategory(@PathVariable categoryName: String): ResponseEntity<Category> {
        return try {
            logger.info("Attempting to delete category: $categoryName (legacy endpoint)")
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

    // ===== BUSINESS LOGIC ENDPOINTS (PRESERVED) =====

    /**
     * Business logic endpoint - PUT /api/category/merge
     * Preserved as-is, not part of standardization
     */
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
