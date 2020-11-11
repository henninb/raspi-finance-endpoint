package finance.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Description
import finance.services.DescriptionService
import org.apache.logging.log4j.LogManager
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.*
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.util.*
import javax.validation.ConstraintViolationException
import javax.validation.ValidationException

@CrossOrigin
@RestController
@RequestMapping("/description")
class DescriptionController(private var descriptionService: DescriptionService) {

    //https://hornsup:8080/description/select/all
    @GetMapping(path = ["/select/all"], produces = ["application/json"])
    fun selectDescription(): ResponseEntity<List<Description>> {
        val descriptions = descriptionService.fetchAllDescriptions()

        return ResponseEntity.ok(descriptions)
    }

    //curl --header "Content-Type: application/json" -X POST -d '{"description":"test", "activeStatus":true}' https://hornsup:8080/description/insert
    @PostMapping(path = ["/insert"], produces = ["application/json"])
    fun insertDescription(@RequestBody description: Description): ResponseEntity<String> {
        descriptionService.insertDescription(description)
        logger.info("description inserted")
        return ResponseEntity.ok("description inserted")
    }

    @DeleteMapping(path = ["/delete/{description}"], produces = ["application/json"])
    fun deleteByDescription(@PathVariable description: String): ResponseEntity<String> {
        descriptionService.deleteByDescription(description)
        logger.info("description deleted")
        return ResponseEntity.ok("payment deleted")
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST) //400
    @ExceptionHandler(value = [ConstraintViolationException::class, NumberFormatException::class,
        MethodArgumentTypeMismatchException::class, HttpMessageNotReadableException::class, ValidationException::class])
    fun handleBadHttpRequests(throwable: Throwable): Map<String, String>? {
        val response: MutableMap<String, String> = HashMap()
        logger.error("Bad Request", throwable)
        response["response"] = "BAD_REQUEST: " + throwable.javaClass.simpleName + " , message: " + throwable.message
        return response
    }

    companion object {
        private val mapper = ObjectMapper()
        private val logger = LogManager.getLogger()
    }
}
