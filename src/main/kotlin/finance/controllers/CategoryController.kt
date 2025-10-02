package finance.controllers

import finance.domain.Category
import finance.domain.ServiceResult
import finance.services.StandardizedCategoryService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@CrossOrigin
@RestController
@RequestMapping("/api/category")
class CategoryController(private val standardizedCategoryService: StandardizedCategoryService) :
    StandardizedBaseController() {
    // ===== STANDARDIZED ENDPOINTS (NEW) =====

    /**
     * Standardized collection retrieval - GET /api/category/active
     * Returns empty list instead of throwing 404 (standardized behavior)
     */
    @GetMapping("/active", produces = ["application/json"])
    fun findAllActive(): ResponseEntity<*> {
        return when (val result = standardizedCategoryService.findAllActive()) {
            is ServiceResult.Success -> {
                logger.info("Retrieved ${result.data.size} active categories")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("No categories found")
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "No categories found"))
            }
            is ServiceResult.SystemError -> {
                logger.error("System error retrieving categories: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("error" to "Internal server error"))
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("error" to "Internal server error"))
            }
        }
    }

    /**
     * Standardized single entity retrieval - GET /api/category/{categoryName}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @GetMapping("/{categoryName}", produces = ["application/json"])
    fun findById(
        @PathVariable categoryName: String,
    ): ResponseEntity<*> {
        return when (val result = standardizedCategoryService.findByCategoryNameStandardized(categoryName)) {
            is ServiceResult.Success -> {
                logger.info("Retrieved category: $categoryName")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Category not found: $categoryName")
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Category not found: $categoryName"))
            }
            is ServiceResult.SystemError -> {
                logger.error("System error retrieving category $categoryName: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("error" to "Internal server error"))
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("error" to "Internal server error"))
            }
        }
    }

    /**
     * Standardized entity creation - POST /api/category
     * Returns 201 CREATED
     */
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    fun save(
        @Valid @RequestBody category: Category,
    ): ResponseEntity<*> {
        return when (val result = standardizedCategoryService.save(category)) {
            is ServiceResult.Success -> {
                logger.info("Category created successfully: ${category.categoryName}")
                ResponseEntity.status(HttpStatus.CREATED).body(result.data)
            }
            is ServiceResult.ValidationError -> {
                logger.warn("Validation error creating category: ${result.errors}")
                ResponseEntity.badRequest().body(mapOf("errors" to result.errors))
            }
            is ServiceResult.BusinessError -> {
                logger.warn("Business error creating category: ${result.message}")
                // Provide user-friendly message for duplicate key violations
                val userMessage =
                    if (result.errorCode == "DATA_INTEGRITY_VIOLATION") {
                        "Duplicate category found"
                    } else {
                        result.message
                    }
                ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to userMessage))
            }
            is ServiceResult.SystemError -> {
                logger.error("System error creating category: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("error" to "Internal server error"))
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("error" to "Internal server error"))
            }
        }
    }

    /**
     * Standardized entity update - PUT /api/category/{categoryName}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @PutMapping("/{categoryName}", consumes = ["application/json"], produces = ["application/json"])
    fun update(
        @PathVariable categoryName: String,
        @Valid @RequestBody category: Category,
    ): ResponseEntity<*> {
        @Suppress("REDUNDANT_ELSE_IN_WHEN") // Defensive programming: handle unexpected ServiceResult types
        return when (val result = standardizedCategoryService.update(category)) {
            is ServiceResult.Success -> {
                logger.info("Category updated successfully: $categoryName")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Category not found for update: $categoryName")
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Category not found for update: $categoryName"))
            }
            is ServiceResult.ValidationError -> {
                logger.warn("Validation error updating category: ${result.errors}")
                ResponseEntity.badRequest().body(mapOf("errors" to result.errors))
            }
            is ServiceResult.BusinessError -> {
                logger.warn("Business error updating category: ${result.message}")
                // Provide user-friendly message for duplicate key violations
                val userMessage =
                    if (result.errorCode == "DATA_INTEGRITY_VIOLATION") {
                        "Duplicate category found"
                    } else {
                        result.message
                    }
                ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to userMessage))
            }
            is ServiceResult.SystemError -> {
                logger.error("System error updating category: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("error" to "Internal server error"))
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("error" to "Internal server error"))
            }
        }
    }

    /**
     * Standardized entity deletion - DELETE /api/category/{categoryName}
     * Returns 200 OK with deleted entity
     */
    @DeleteMapping("/{categoryName}", produces = ["application/json"])
    fun deleteById(
        @PathVariable categoryName: String,
    ): ResponseEntity<*> {
        // First get the category to return it after deletion
        return when (val findResult = standardizedCategoryService.findByCategoryNameStandardized(categoryName)) {
            is ServiceResult.Success -> {
                when (val deleteResult = standardizedCategoryService.deleteByCategoryNameStandardized(categoryName)) {
                    is ServiceResult.Success -> {
                        logger.info("Category deleted successfully: $categoryName")
                        ResponseEntity.ok(findResult.data)
                    }
                    is ServiceResult.NotFound -> {
                        logger.warn("Category not found for deletion: $categoryName")
                        ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Category not found for deletion: $categoryName"))
                    }
                    is ServiceResult.SystemError -> {
                        logger.error("System error deleting category: ${deleteResult.exception.message}", deleteResult.exception)
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("error" to "Internal server error"))
                    }
                    else -> {
                        logger.error("Unexpected delete result type: $deleteResult")
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("error" to "Internal server error"))
                    }
                }
            }
            is ServiceResult.NotFound -> {
                logger.warn("Category not found for deletion: $categoryName")
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Category not found for deletion: $categoryName"))
            }
            is ServiceResult.SystemError -> {
                logger.error("System error finding category for deletion: ${findResult.exception.message}", findResult.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("error" to "Internal server error"))
            }
            else -> {
                logger.error("Unexpected find result type: $findResult")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("error" to "Internal server error"))
            }
        }
    }

    // ===== LEGACY ENDPOINTS (BACKWARD COMPATIBILITY) =====

    /**
     * Legacy endpoint - GET /api/category/select/active
     * Maintains original behavior: throws 404 if empty
     */
    @GetMapping("/select/active", produces = ["application/json"])
    fun categories(): ResponseEntity<List<Category>> {
        return when (val result = standardizedCategoryService.findAllActive()) {
            is ServiceResult.Success -> {
                if (result.data.isEmpty()) {
                    logger.warn("No categories found in the datastore")
                    throw ResponseStatusException(HttpStatus.NOT_FOUND, "No categories found")
                }
                logger.info("Retrieved ${result.data.size} active categories")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.SystemError -> {
                logger.error("Failed to retrieve categories: ${result.exception.message}", result.exception)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve categories: ${result.exception.message}", result.exception)
            }
            else -> {
                logger.error("Unexpected result type: $result")
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve categories")
            }
        }
    }

    /**
     * Legacy endpoint - GET /api/category/select/{category_name}
     * Maintains snake_case @PathVariable annotation for backward compatibility
     */
    @GetMapping("/select/{category_name}")
    fun category(
        @PathVariable("category_name") categoryName: String,
    ): ResponseEntity<Category> {
        return when (val result = standardizedCategoryService.findByCategoryNameStandardized(categoryName)) {
            is ServiceResult.Success -> {
                logger.info("Retrieved category: ${result.data.categoryName}")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Category not found: $categoryName")
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found: $categoryName")
            }
            is ServiceResult.SystemError -> {
                logger.error("Failed to retrieve category $categoryName: ${result.exception.message}", result.exception)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve category: ${result.exception.message}", result.exception)
            }
            else -> {
                logger.error("Unexpected result type: $result")
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve category")
            }
        }
    }

    /**
     * Legacy endpoint - POST /api/category/insert
     */
    @PostMapping("/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insertCategory(
        @RequestBody category: Category,
    ): ResponseEntity<Category> {
        return when (val result = standardizedCategoryService.save(category)) {
            is ServiceResult.Success -> {
                logger.info("Category inserted successfully: ${result.data.categoryName}")
                ResponseEntity(result.data, HttpStatus.CREATED)
            }
            is ServiceResult.ValidationError -> {
                logger.error("Validation error inserting category ${category.categoryName}: ${result.errors}")
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Validation error: ${result.errors}")
            }
            is ServiceResult.BusinessError -> {
                logger.error("Failed to insert category due to business error: ${result.message}")
                throw ResponseStatusException(HttpStatus.CONFLICT, "Duplicate category found.")
            }
            is ServiceResult.SystemError -> {
                logger.error("Unexpected error inserting category ${category.categoryName}: ${result.exception.message}", result.exception)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: ${result.exception.message}", result.exception)
            }
            else -> {
                logger.error("Unexpected result type: $result")
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error")
            }
        }
    }

    /**
     * Legacy endpoint - PUT /api/category/update/{category_name}
     * Maintains snake_case @PathVariable annotation for backward compatibility
     */
    @PutMapping("/update/{category_name}", consumes = ["application/json"], produces = ["application/json"])
    fun updateCategory(
        @PathVariable("category_name") categoryName: String,
        @RequestBody toBePatchedCategory: Category,
    ): ResponseEntity<Category> {
        @Suppress("REDUNDANT_ELSE_IN_WHEN") // Defensive programming: handle unexpected ServiceResult types
        return when (val result = standardizedCategoryService.update(toBePatchedCategory)) {
            is ServiceResult.Success -> {
                logger.info("Category updated successfully: $categoryName")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Category not found for update: $categoryName")
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found: $categoryName")
            }
            is ServiceResult.ValidationError -> {
                logger.error("Validation error updating category $categoryName: ${result.errors}")
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Validation error: ${result.errors}")
            }
            is ServiceResult.BusinessError -> {
                logger.error("Business error updating category $categoryName: ${result.message}")
                throw ResponseStatusException(HttpStatus.CONFLICT, "Business error: ${result.message}")
            }
            is ServiceResult.SystemError -> {
                logger.error("Failed to update category $categoryName: ${result.exception.message}", result.exception)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update category: ${result.exception.message}", result.exception)
            }
            else -> {
                logger.error("Unexpected result type: $result")
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred")
            }
        }
    }

    /**
     * Legacy endpoint - DELETE /api/category/delete/{categoryName}
     */
    @DeleteMapping("/delete/{categoryName}", produces = ["application/json"])
    fun deleteCategory(
        @PathVariable categoryName: String,
    ): ResponseEntity<Category> {
        // First get the category to return it after deletion
        return when (val findResult = standardizedCategoryService.findByCategoryNameStandardized(categoryName)) {
            is ServiceResult.Success -> {
                when (val deleteResult = standardizedCategoryService.deleteByCategoryNameStandardized(categoryName)) {
                    is ServiceResult.Success -> {
                        logger.info("Category deleted successfully: $categoryName")
                        ResponseEntity.ok(findResult.data)
                    }
                    is ServiceResult.NotFound -> {
                        logger.warn("Category not found for deletion: $categoryName")
                        throw ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found: $categoryName")
                    }
                    is ServiceResult.SystemError -> {
                        logger.error("Failed to delete category $categoryName: ${deleteResult.exception.message}", deleteResult.exception)
                        throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete category: ${deleteResult.exception.message}", deleteResult.exception)
                    }
                    else -> {
                        logger.error("Unexpected delete result type: $deleteResult")
                        throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete category")
                    }
                }
            }
            is ServiceResult.NotFound -> {
                logger.warn("Category not found for deletion: $categoryName")
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found: $categoryName")
            }
            is ServiceResult.SystemError -> {
                logger.error("Failed to find category for deletion $categoryName: ${findResult.exception.message}", findResult.exception)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete category: ${findResult.exception.message}", findResult.exception)
            }
            else -> {
                logger.error("Unexpected find result type: $findResult")
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete category")
            }
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
        @RequestParam("old") categoryName2: String,
    ): ResponseEntity<Category> {
        return try {
            logger.info("Merging categories: $categoryName2 into $categoryName1")
            val mergedCategory = standardizedCategoryService.mergeCategories(categoryName1, categoryName2)
            logger.info("Categories merged successfully: $categoryName2 into $categoryName1")
            ResponseEntity.ok(mergedCategory)
        } catch (ex: Exception) {
            logger.error("Failed to merge categories $categoryName2 into $categoryName1: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to merge categories: ${ex.message}", ex)
        }
    }
}
