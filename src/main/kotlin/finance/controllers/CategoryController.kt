package finance.controllers

import finance.models.Category
import finance.services.CategoryService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class CategoryController {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    @Autowired
    lateinit var categoryService: CategoryService

    //curl --header "Content-Type: application/json" --request POST --data '{"category":"test"}' http://localhost:8080/insert_category
    @PostMapping(path = [("/insert_category")], consumes = [("application/json")], produces = [("application/json")])
    fun insert_category(@RequestBody category: Category) : ResponseEntity<String> {
        categoryService.insertCategory(category)
        logger.info("insert_category")
        return ResponseEntity.ok("category inserted")
    }
}