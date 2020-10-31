package finance.controllers

import finance.domain.Description
import finance.services.DescriptionService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.*
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException
import java.util.*
import javax.validation.ConstraintViolationException

@CrossOrigin
@RestController
@RequestMapping("/description")
class DescriptionController(private var descriptionService: DescriptionService) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    //https://hornsup:8080/description/select/all
    @GetMapping(path = ["/select/all"], produces = ["application/json"])
    fun selectDescription(): ResponseEntity<List<Description>> {
        val descriptions  = descriptionService.fetchAllDescriptions()

        return ResponseEntity.ok(descriptions)
    }

    //curl --header "Content-Type: application/json" -X POST -d '{"description":"test"}' http://localhost:8080/description/insert
    @PostMapping(path = ["/insert"], produces = ["application/json"])
    fun insertDescription(@RequestBody description: Description): ResponseEntity<String> {
        descriptionService.insertDescription(description)
        logger.info("insertDescription")
        return ResponseEntity.ok("description inserted")
    }

    @DeleteMapping(path = ["/delete/{description}"], produces = ["application/json"])
    fun deleteByDescription(@PathVariable description: String): ResponseEntity<String> {
        descriptionService.deleteByDescription(description)
        return ResponseEntity.ok("payment deleted")
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST) //400
    @ExceptionHandler(value = [ConstraintViolationException::class, NumberFormatException::class, MethodArgumentTypeMismatchException::class, HttpMessageNotReadableException::class])
    fun handleBadHttpRequests(throwable: Throwable): Map<String, String>? {
        val response: MutableMap<String, String> = HashMap()
        logger.error("Bad Request", throwable)
        response["response"] = "BAD_REQUEST: " + throwable.javaClass.simpleName + " , message: " + throwable.message
        return response
    }
}