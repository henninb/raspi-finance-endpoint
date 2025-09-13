package finance.controllers

import finance.domain.Description
import finance.services.DescriptionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import jakarta.validation.Valid
import java.util.*

@CrossOrigin
@RestController
@RequestMapping("/api/description")
class DescriptionController(private val descriptionService: DescriptionService) :
    StandardizedBaseController(), StandardRestController<Description, String> {

    // ===== STANDARDIZED ENDPOINTS (NEW) =====

    /**
     * Standardized collection retrieval - GET /api/description/active
     * Returns empty list instead of throwing 404 (standardized behavior)
     */
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<Description>> {
        return handleCrudOperation("Find all active descriptions", null) {
            logger.debug("Retrieving all active descriptions")
            val descriptions: List<Description> = descriptionService.fetchAllDescriptions()
            logger.info("Retrieved ${descriptions.size} active descriptions")
            descriptions
        }
    }

    /**
     * Standardized single entity retrieval - GET /api/description/{descriptionName}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @GetMapping("/{descriptionName}", produces = ["application/json"])
    override fun findById(@PathVariable descriptionName: String): ResponseEntity<Description> {
        return handleCrudOperation("Find description by name", descriptionName) {
            logger.debug("Retrieving description: $descriptionName")
            val description = descriptionService.findByDescriptionName(descriptionName)
                .orElseThrow {
                    logger.warn("Description not found: $descriptionName")
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Description not found: $descriptionName")
                }
            logger.info("Retrieved description: $descriptionName")
            description
        }
    }

    /**
     * Standardized entity creation - POST /api/description
     * Returns 201 CREATED
     */
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    override fun save(@Valid @RequestBody description: Description): ResponseEntity<Description> {
        return handleCreateOperation("Description", description.descriptionName) {
            logger.info("Creating description: ${description.descriptionName}")
            val result = descriptionService.insertDescription(description)
            logger.info("Description created successfully: ${description.descriptionName}")
            result
        }
    }

    /**
     * Standardized entity update - PUT /api/description/{descriptionName}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @PutMapping("/{descriptionName}", consumes = ["application/json"], produces = ["application/json"])
    override fun update(@PathVariable descriptionName: String, @Valid @RequestBody description: Description): ResponseEntity<Description> {
        return handleCrudOperation("Update description", descriptionName) {
            logger.info("Updating description: $descriptionName")
            // Validate description exists first
            descriptionService.findByDescriptionName(descriptionName)
                .orElseThrow {
                    logger.warn("Description not found for update: $descriptionName")
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Description not found: $descriptionName")
                }
            val result = descriptionService.updateDescription(description)
            logger.info("Description updated successfully: $descriptionName")
            result
        }
    }

    /**
     * Standardized entity deletion - DELETE /api/description/{descriptionName}
     * Returns 200 OK with deleted entity
     */
    @DeleteMapping("/{descriptionName}", produces = ["application/json"])
    override fun deleteById(@PathVariable descriptionName: String): ResponseEntity<Description> {
        return handleDeleteOperation(
            "Description",
            descriptionName,
            { descriptionService.findByDescriptionName(descriptionName) },
            { descriptionService.deleteByDescriptionName(descriptionName) }
        )
    }

    // ===== LEGACY ENDPOINTS (BACKWARD COMPATIBILITY) =====

    /**
     * Legacy endpoint - GET /api/description/select/active
     * Maintains original behavior
     */
    @GetMapping("/select/active", produces = ["application/json"])
    fun selectAllDescriptions(): ResponseEntity<List<Description>> {
        return try {
            logger.debug("Retrieving all descriptions (legacy endpoint)")
            val descriptions = descriptionService.fetchAllDescriptions()
            logger.info("Retrieved ${descriptions.size} descriptions")
            ResponseEntity.ok(descriptions)
        } catch (ex: Exception) {
            logger.error("Failed to retrieve descriptions: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve descriptions: ${ex.message}", ex)
        }
    }

    /**
     * Legacy endpoint - PUT /api/description/update/{description_name}
     * Maintains snake_case @PathVariable annotation for backward compatibility
     */
    @PutMapping("/update/{description_name}", consumes = ["application/json"], produces = ["application/json"])
    fun updateDescription(
        @PathVariable("description_name") descriptionName: String,
        @RequestBody toBePatchedDescription: Description
    ): ResponseEntity<Description> {
        return try {
            logger.info("Updating description: $descriptionName (legacy endpoint)")
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

    /**
     * Legacy endpoint - GET /api/description/select/{description_name}
     * Maintains snake_case @PathVariable annotation for backward compatibility
     */
    @GetMapping("/select/{description_name}")
    fun selectDescriptionName(@PathVariable("description_name") descriptionName: String): ResponseEntity<Description> {
        return try {
            logger.debug("Retrieving description: $descriptionName (legacy endpoint)")
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

    /**
     * Legacy endpoint - POST /api/description/insert
     */
    @PostMapping("/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insertDescription(@RequestBody description: Description): ResponseEntity<Description> {
        return try {
            logger.info("Inserting description: ${description.descriptionName} (legacy endpoint)")
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

    /**
     * Legacy endpoint - DELETE /api/description/delete/{descriptionName}
     */
    @DeleteMapping("/delete/{descriptionName}", produces = ["application/json"])
    fun deleteByDescription(@PathVariable descriptionName: String): ResponseEntity<Description> {
        return try {
            logger.info("Attempting to delete description: $descriptionName (legacy endpoint)")
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

    // ===== BUSINESS LOGIC ENDPOINTS (PRESERVED) =====


    /**
     * Business logic endpoint - POST /api/description/merge
     * Preserved as-is, not part of standardization
     */
    @PostMapping("/merge", consumes = ["application/json"], produces = ["application/json"])
    fun mergeDescriptions(@RequestBody request: MergeDescriptionsRequest): ResponseEntity<Description> {
        return try {
            if (request.targetName.isBlank() || request.sourceNames.isEmpty()) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "targetName and sourceNames are required")
            }
            logger.info("Merging descriptions ${request.sourceNames} into ${request.targetName}")
            val merged = descriptionService.mergeDescriptions(request.targetName, request.sourceNames)
            ResponseEntity.ok(merged)
        } catch (ex: ResponseStatusException) {
            throw ex
        } catch (ex: Exception) {
            logger.error("Failed to merge descriptions into ${request.targetName}: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to merge descriptions: ${ex.message}", ex)
        }
    }
}
