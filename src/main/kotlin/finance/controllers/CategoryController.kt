package finance.controllers

import finance.domain.Category
import finance.domain.toCreatedResponse
import finance.domain.toListOkResponse
import finance.domain.toOkResponse
import finance.domain.toPagedOkResponse
import finance.services.CategoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
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

@Tag(name = "Category Management", description = "Operations for managing categories")
@RestController
@RequestMapping("/api/category")
@PreAuthorize("hasAuthority('USER')")
class CategoryController(
    private val categoryService: CategoryService,
) : StandardizedBaseController(),
    StandardRestController<Category, String> {
    @Operation(summary = "Get all active categories")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Active categories retrieved"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<Category>> = categoryService.findAllActive().toListOkResponse()

    @Operation(summary = "Get all active categories (paginated)")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Page of categories returned"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/active/paged", produces = ["application/json"])
    override fun findAllActivePaged(pageable: Pageable): ResponseEntity<Page<Category>> = categoryService.findAllActive(pageable).toPagedOkResponse(pageable)

    @Operation(summary = "Get category by name")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Category retrieved"),
            ApiResponse(responseCode = "404", description = "Category not found"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @GetMapping("/{categoryName}", produces = ["application/json"])
    override fun findById(
        @PathVariable("categoryName") id: String,
    ): ResponseEntity<Category> = categoryService.findByCategoryNameStandardized(id).toOkResponse()

    @Operation(summary = "Create category")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Category created"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "409", description = "Conflict/duplicate"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    override fun save(
        @Valid @RequestBody entity: Category,
    ): ResponseEntity<Category> = categoryService.save(entity).toCreatedResponse()

    @Operation(summary = "Update category by name")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Category updated"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "404", description = "Category not found"),
            ApiResponse(responseCode = "409", description = "Conflict"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @PutMapping("/{categoryName}", consumes = ["application/json"], produces = ["application/json"])
    override fun update(
        @PathVariable("categoryName") id: String,
        @Valid @RequestBody entity: Category,
    ): ResponseEntity<Category> = categoryService.update(entity).toOkResponse()

    @Operation(summary = "Delete category by name")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Category deleted"),
            ApiResponse(responseCode = "404", description = "Category not found"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    @DeleteMapping("/{categoryName}", produces = ["application/json"])
    override fun deleteById(
        @PathVariable("categoryName") id: String,
    ): ResponseEntity<Category> = categoryService.deleteByCategoryNameStandardized(id).toOkResponse()

    // ===== BUSINESS LOGIC ENDPOINTS =====

    @PutMapping("/merge", produces = ["application/json"])
    @Operation(summary = "Merge one category into another")
    @ApiResponses(value = [ApiResponse(responseCode = "200", description = "Categories merged"), ApiResponse(responseCode = "500", description = "Internal server error")])
    fun mergeCategories(
        @RequestParam(value = "new") categoryName1: String,
        @RequestParam("old") categoryName2: String,
    ): ResponseEntity<Category> =
        try {
            ResponseEntity.ok(categoryService.mergeCategories(categoryName1, categoryName2))
        } catch (ex: Exception) {
            logger.error("Failed to merge categories $categoryName2 into $categoryName1: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to merge categories: ${ex.message}", ex)
        }
}
