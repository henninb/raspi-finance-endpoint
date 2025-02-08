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
class CategoryController(private var categoryService: CategoryService) : BaseController() {

    //http://localhost:8443/category/select/active
    @GetMapping("/select/active", produces = ["application/json"])
    fun categories(): ResponseEntity<List<Category>> {
        val categories: List<Category> = categoryService.categories()
        if (categories.isEmpty()) {
            logger.error("no categories found in the datastore.")
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "could not find any categories in the datastore.")
        }
        logger.info("select active categories: ${categories.size}")
        return ResponseEntity.ok(categories)
    }

    @GetMapping("/select/{category_name}")
    fun category(@PathVariable("category_name") categoryName: String): ResponseEntity<Category> {
        val categoryOptional = categoryService.category(categoryName)
        if (categoryOptional.isPresent) {
            val category = categoryOptional.get()
            logger.info("cattegory deleted: ${category.categoryName}")
            return ResponseEntity.ok(category)
        }
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "category not found for: $categoryName")
    }

    @PutMapping("/update/{category_name}", consumes = ["application/json"], produces = ["application/json"])
    fun updateCategory(
        @PathVariable("category_name") categoryName: String,
        @RequestBody toBePatchedCategory: Category
    ): ResponseEntity<Category> {
        val categoryOptional = categoryService.findByCategoryName(categoryName)
        if (categoryOptional.isPresent) {
            val categoryResponse = categoryService.updateCategory(toBePatchedCategory)
            return ResponseEntity.ok(categoryResponse)
        }
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found for: $categoryName")
    }

    //curl --header "Content-Type: application/json" -X POST -d '{"category":"test"}' http://localhost:8443/category/insert
    @PostMapping("/insert", produces = ["application/json"])
    fun insertCategory(@RequestBody category: Category): ResponseEntity<Category> {
        val categoryResponse = categoryService.insertCategory(category)
        return ResponseEntity.ok(categoryResponse)
    }

    @DeleteMapping("/delete/{categoryName}", produces = ["application/json"])
    fun deleteCategory(@PathVariable categoryName: String): ResponseEntity<Category> {
        val categoryOptional: Optional<Category> = categoryService.findByCategoryName(categoryName)

        if (categoryOptional.isPresent) {
            val categoryToDelete = categoryOptional.get() // Get the category object
            categoryService.deleteCategory(categoryName)
            return ResponseEntity.ok(categoryToDelete) // Return the deleted category
        }

        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not delete this category: $categoryName.")
    }
}
