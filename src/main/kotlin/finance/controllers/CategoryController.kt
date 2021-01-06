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
@RequestMapping("/category")
class CategoryController(private var categoryService: CategoryService) : BaseController() {

    //http://localhost:8080/category/select/active
    @GetMapping(path = ["/select/active"], produces = ["application/json"])
    fun selectAllActiveCategories(): ResponseEntity<List<Category>> {
        val categories: List<Category> = categoryService.fetchAllActiveCategories()
        if (categories.isEmpty()) {
            logger.error("no categories found in the datastore.")
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "could not find any categories in the datastore.")
        }
        logger.info("select active categories: ${categories.size}")
        return ResponseEntity.ok(categories)
    }

    @GetMapping(path = ["/select/{category_name}"])
    fun selectCategoryName(@PathVariable("category_name") categoryName: String): ResponseEntity<String> {
        val categoryOptional = categoryService.findByCategory(categoryName)
        if (categoryOptional.isPresent) {
            return ResponseEntity.ok(mapper.writeValueAsString(categoryOptional.get()))
        }
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "category not found for: $categoryName")
    }

    //curl --header "Content-Type: application/json" -X POST -d '{"category":"test"}' http://localhost:8080/category/insert
    @PostMapping(path = ["/insert"], produces = ["application/json"])
    fun insertCategory(@RequestBody category: Category): ResponseEntity<String> {
        categoryService.insertCategory(category)
        logger.info("insertCategory")
        return ResponseEntity.ok("category inserted")
    }

    @DeleteMapping(path = ["/delete/{categoryName}"], produces = ["application/json"])
    fun deleteByCategoryName(@PathVariable categoryName: String): ResponseEntity<String> {
        val categoryOptional: Optional<Category> = categoryService.findByCategoryName(categoryName)

        if (categoryOptional.isPresent) {
            categoryService.deleteByCategoryName(categoryName)
            return ResponseEntity.ok("category deleted")
        }

        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "could not delete this category: $categoryName.")
    }
}
