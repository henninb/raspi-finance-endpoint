package finance.controllers

import finance.domain.Description
import finance.services.DescriptionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.*

@CrossOrigin
@RestController
@RequestMapping("/description", "/api/description")
class DescriptionController(private var descriptionService: DescriptionService) : BaseController() {

    //https://hornsup:8443/description/select/active
    @GetMapping("/select/active", produces = ["application/json"])
    fun selectAllDescriptions(): ResponseEntity<List<Description>> {
        val descriptions = descriptionService.fetchAllDescriptions()

        return ResponseEntity.ok(descriptions)
    }

    @PutMapping("/update/{description_name}", consumes = ["application/json"], produces = ["application/json"])
    fun updateDescription(
        @PathVariable("description_name") descriptionName: String,
        @RequestBody toBePatchedDescription: Description
    ): ResponseEntity<Description> {
        val descriptionOptional = descriptionService.findByDescriptionName(descriptionName)
        if (descriptionOptional.isPresent) {
            val description = descriptionOptional.get()
            val descriptionResponse = descriptionService.updateDescription(description)
            return ResponseEntity.ok(descriptionResponse)
        }
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "description not found for: $descriptionName")
    }

    //curl -k 'https://localhost:8443/description/select/zzz'
    @GetMapping("/select/{description_name}")
    fun selectDescriptionName(@PathVariable("description_name") descriptionName: String): ResponseEntity<String> {
        val descriptionOptional = descriptionService.findByDescriptionName(descriptionName)
        if (descriptionOptional.isPresent) {
            return ResponseEntity.ok(mapper.writeValueAsString(descriptionOptional.get()))
        }
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "description not found for: $descriptionName")
    }

    //curl -k --header "Content-Type: application/json" -X POST -d '{"description":"test", "activeStatus":true}' 'https://hornsup:8443/description/insert'
    @PostMapping("/insert", produces = ["application/json"])
    fun insertDescription(@RequestBody description: Description): ResponseEntity<Description> {
        val descriptionResponse = descriptionService.insertDescription(description)
        return ResponseEntity.ok(descriptionResponse)
    }

    @DeleteMapping("/delete/{descriptionName}", produces = ["application/json"])
    fun deleteByDescription(@PathVariable descriptionName: String): ResponseEntity<Description> {
        val descriptionOptional: Optional<Description> = descriptionService.findByDescriptionName(descriptionName)

        if (descriptionOptional.isPresent) {
            descriptionService.deleteByDescriptionName(descriptionName)
            val description = descriptionOptional.get()
            logger.info("description deleted: ${description.descriptionName}")
            return ResponseEntity.ok(description)
        }

        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "could not delete the description: $descriptionName.")
    }
}
