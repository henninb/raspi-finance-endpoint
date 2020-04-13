package finance.controllers

import finance.domain.Category
import finance.services.CategoryService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/category")
//@Validated
open class CategoryController @Autowired constructor(private var categoryService: CategoryService) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    //curl --header "Content-Type: application/json" -X POST -d '{"category":"test"}' http://localhost:8080/category/insert
    @PostMapping(path = ["/insert_category"])
    fun insertCategory(@RequestBody category: Category) : ResponseEntity<String> {
        categoryService.insertCategory(category)
        logger.info("insertCategory")
        return ResponseEntity.ok("category inserted")
    }
}
