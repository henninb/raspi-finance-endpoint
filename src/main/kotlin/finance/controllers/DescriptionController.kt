package finance.controllers

import finance.domain.Description
import finance.services.DescriptionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@CrossOrigin
@RestController
@RequestMapping("/description")
class DescriptionController(private var descriptionService: DescriptionService) : BaseController() {

    //https://hornsup:8080/description/select/all
    @GetMapping(path = ["/select/all"], produces = ["application/json"])
    fun selectAllDescriptions(): ResponseEntity<List<Description>> {
        val descriptions = descriptionService.fetchAllDescriptions()

        return ResponseEntity.ok(descriptions)
    }

    //curl -k --header "Content-Type: application/json" -X POST -d '{"description":"test", "activeStatus":true}' 'https://hornsup:8080/description/insert'
    @PostMapping(path = ["/insert"], produces = ["application/json"])
    fun insertDescription(@RequestBody description: Description): ResponseEntity<String> {
        descriptionService.insertDescription(description)
        logger.info("description inserted")
        return ResponseEntity.ok("description inserted")
    }

    //curl -k 'https://localhost:8080/description/select/zzz'
    @GetMapping(path = ["/select/{description_name}"])
    fun selectDescriptionName(@PathVariable("description_name") descriptionName: String): ResponseEntity<String> {
        val descriptionOptional = descriptionService.findByDescriptionName(descriptionName)
        if (descriptionOptional.isPresent) {
            return ResponseEntity.ok(mapper.writeValueAsString(descriptionOptional.get()))
        }
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "description not found for: $descriptionName")
    }

    @DeleteMapping(path = ["/delete/{description}"], produces = ["application/json"])
    fun deleteByDescription(@PathVariable description: String): ResponseEntity<String> {
        descriptionService.deleteByDescription(description)
        logger.info("description deleted")
        return ResponseEntity.ok("payment deleted")
    }
}
