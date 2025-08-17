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
class DescriptionController(private val descriptionService: DescriptionService) : BaseController() {

    // curl -k https://localhost:8443/description/select/active
    @GetMapping("/select/active", produces = ["application/json"])
    fun selectAllDescriptions(): ResponseEntity<List<Description>> {
        return try {
            logger.debug("Retrieving all descriptions")
            val descriptions = descriptionService.fetchAllDescriptions()
            logger.info("Retrieved ${descriptions.size} descriptions")
            ResponseEntity.ok(descriptions)
        } catch (ex: Exception) {
            logger.error("Failed to retrieve descriptions: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve descriptions: ${ex.message}", ex)
        }
    }

    // curl -k --header "Content-Type: application/json" --request PUT --data '{"descriptionName":"amazon", "activeStatus": true}' https://localhost:8443/description/update/amazon
    @PutMapping("/update/{description_name}", consumes = ["application/json"], produces = ["application/json"])
    fun updateDescription(
        @PathVariable("description_name") descriptionName: String,
        @RequestBody toBePatchedDescription: Description
    ): ResponseEntity<Description> {
        return try {
            logger.info("Updating description: $descriptionName")
            descriptionService.findByDescriptionName(descriptionName)
                .orElseThrow {
                    logger.warn("Description not found for update: $descriptionName")
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Description not found: $descriptionName")
                }
            val descriptionResponse = descriptionService.updateDescription(toBePatchedDescription)
            logger.info("Description updated successfully: $descriptionName")
            ResponseEntity.ok(descriptionResponse)
        } catch (ex: ResponseStatusException) {
            throw ex
        } catch (ex: Exception) {
            logger.error("Failed to update description $descriptionName: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update description: ${ex.message}", ex)
        }
    }

    // curl -k https://localhost:8443/description/select/amazon
    @GetMapping("/select/{description_name}")
    fun selectDescriptionName(@PathVariable("description_name") descriptionName: String): ResponseEntity<Description> {
        return try {
            logger.debug("Retrieving description: $descriptionName")
            val description = descriptionService.findByDescriptionName(descriptionName)
                .orElseThrow {
                    logger.warn("Description not found: $descriptionName")
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Description not found: $descriptionName")
                }
            logger.info("Retrieved description: $descriptionName")
            ResponseEntity.ok(description)
        } catch (ex: ResponseStatusException) {
            throw ex
        } catch (ex: Exception) {
            logger.error("Failed to retrieve description $descriptionName: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve description: ${ex.message}", ex)
        }
    }

    // curl -k --header "Content-Type: application/json" --request POST --data '{"descriptionName":"test", "activeStatus": true}' https://localhost:8443/description/insert
    @PostMapping("/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insertDescription(@RequestBody description: Description): ResponseEntity<Description> {
        return try {
            logger.info("Inserting description: ${description.descriptionName}")
            val descriptionResponse = descriptionService.insertDescription(description)
            logger.info("Description inserted successfully: ${descriptionResponse.descriptionName}")
            ResponseEntity(descriptionResponse, HttpStatus.CREATED)
        } catch (ex: org.springframework.dao.DataIntegrityViolationException) {
            logger.error("Failed to insert description due to data integrity violation: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.CONFLICT, "Duplicate description found.")
        } catch (ex: jakarta.validation.ValidationException) {
            logger.error("Validation error inserting description ${description.descriptionName}: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Validation error: ${ex.message}", ex)
        } catch (ex: IllegalArgumentException) {
            logger.error("Invalid input inserting description ${description.descriptionName}: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input: ${ex.message}", ex)
        } catch (ex: Exception) {
            logger.error("Unexpected error inserting description ${description.descriptionName}: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: ${ex.message}", ex)
        }
    }

    // curl -k --header "Content-Type: application/json" --request DELETE https://localhost:8443/description/delete/test
    @DeleteMapping("/delete/{descriptionName}", produces = ["application/json"])
    fun deleteByDescription(@PathVariable descriptionName: String): ResponseEntity<Description> {
        return try {
            logger.info("Attempting to delete description: $descriptionName")
            val description = descriptionService.findByDescriptionName(descriptionName)
                .orElseThrow {
                    logger.warn("Description not found for deletion: $descriptionName")
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Description not found: $descriptionName")
                }

            descriptionService.deleteByDescriptionName(descriptionName)
            logger.info("Description deleted successfully: $descriptionName")
            ResponseEntity.ok(description)
        } catch (ex: ResponseStatusException) {
            throw ex
        } catch (ex: Exception) {
            logger.error("Failed to delete description $descriptionName: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete description: ${ex.message}", ex)
        }
    }
}
