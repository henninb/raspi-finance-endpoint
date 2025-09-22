package finance.controllers

import finance.domain.Description
import finance.domain.MergeDescriptionsRequest
import finance.domain.ServiceResult
import finance.services.StandardizedDescriptionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import jakarta.validation.Valid
import java.util.*

@CrossOrigin
@RestController
@RequestMapping("/api/description")
class DescriptionController(private val standardizedDescriptionService: StandardizedDescriptionService) :
    StandardizedBaseController(), StandardRestController<Description, String> {

    // ===== STANDARDIZED ENDPOINTS (NEW) =====

    /**
     * Standardized collection retrieval - GET /api/description/active
     * Returns empty list instead of throwing 404 (standardized behavior)
     */
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<Description>> {
        return when (val result = standardizedDescriptionService.findAllActive()) {
            is ServiceResult.Success -> {
                logger.info("Retrieved ${result.data.size} active descriptions")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("No descriptions found")
                ResponseEntity.notFound().build()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error retrieving descriptions: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    /**
     * Standardized single entity retrieval - GET /api/description/{descriptionName}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @GetMapping("/{descriptionName}", produces = ["application/json"])
    override fun findById(@PathVariable descriptionName: String): ResponseEntity<Description> {
        return when (val result = standardizedDescriptionService.findByDescriptionNameStandardized(descriptionName)) {
            is ServiceResult.Success -> {
                logger.info("Retrieved description: $descriptionName")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Description not found: $descriptionName")
                ResponseEntity.notFound().build()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error retrieving description $descriptionName: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    /**
     * Standardized entity creation - POST /api/description
     * Returns 201 CREATED
     */
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    override fun save(@Valid @RequestBody description: Description): ResponseEntity<Description> {
        return when (val result = standardizedDescriptionService.save(description)) {
            is ServiceResult.Success -> {
                logger.info("Description created successfully: ${description.descriptionName}")
                ResponseEntity.status(HttpStatus.CREATED).body(result.data)
            }
            is ServiceResult.ValidationError -> {
                logger.warn("Validation error creating description: ${result.errors}")
                ResponseEntity.badRequest().build<Description>()
            }
            is ServiceResult.BusinessError -> {
                logger.warn("Business error creating description: ${result.message}")
                ResponseEntity.status(HttpStatus.CONFLICT).build<Description>()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error creating description: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Description>()
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    /**
     * Standardized entity update - PUT /api/description/{descriptionName}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @PutMapping("/{descriptionName}", consumes = ["application/json"], produces = ["application/json"])
    override fun update(@PathVariable descriptionName: String, @Valid @RequestBody description: Description): ResponseEntity<Description> {
        return when (val result = standardizedDescriptionService.update(description)) {
            is ServiceResult.Success -> {
                logger.info("Description updated successfully: $descriptionName")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Description not found for update: $descriptionName")
                ResponseEntity.notFound().build<Description>()
            }
            is ServiceResult.ValidationError -> {
                logger.warn("Validation error updating description: ${result.errors}")
                ResponseEntity.badRequest().build<Description>()
            }
            is ServiceResult.BusinessError -> {
                logger.warn("Business error updating description: ${result.message}")
                ResponseEntity.status(HttpStatus.CONFLICT).build<Description>()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error updating description $descriptionName: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Description>()
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    /**
     * Standardized entity deletion - DELETE /api/description/{descriptionName}
     * Returns 200 OK with deleted entity
     */
    @DeleteMapping("/{descriptionName}", produces = ["application/json"])
    override fun deleteById(@PathVariable descriptionName: String): ResponseEntity<Description> {
        // First find the description to return it
        return when (val findResult = standardizedDescriptionService.findByDescriptionNameStandardized(descriptionName)) {
            is ServiceResult.Success -> {
                val description = findResult.data
                when (val deleteResult = standardizedDescriptionService.deleteByDescriptionNameStandardized(descriptionName)) {
                    is ServiceResult.Success -> {
                        logger.info("Description deleted successfully: $descriptionName")
                        ResponseEntity.ok(description)
                    }
                    is ServiceResult.NotFound -> {
                        logger.warn("Description not found for deletion: $descriptionName")
                        ResponseEntity.notFound().build<Description>()
                    }
                    is ServiceResult.SystemError -> {
                        logger.error("System error deleting description $descriptionName: ${deleteResult.exception.message}", deleteResult.exception)
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Description>()
                    }
                    else -> {
                        logger.error("Unexpected delete result type: $deleteResult")
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                    }
                }
            }
            is ServiceResult.NotFound -> {
                logger.warn("Description not found for deletion: $descriptionName")
                ResponseEntity.notFound().build<Description>()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error finding description $descriptionName: ${findResult.exception.message}", findResult.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Description>()
            }
            else -> {
                logger.error("Unexpected find result type: $findResult")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    // ===== LEGACY ENDPOINTS (BACKWARD COMPATIBILITY) =====

    /**
     * Legacy endpoint - GET /api/description/select/active
     * Maintains original behavior
     */
    @GetMapping("/select/active", produces = ["application/json"])
    fun selectAllDescriptions(): ResponseEntity<List<Description>> {
        return when (val result = standardizedDescriptionService.findAllActive()) {
            is ServiceResult.Success -> {
                logger.info("Retrieved ${result.data.size} descriptions (legacy endpoint)")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.SystemError -> {
                logger.error("Failed to retrieve descriptions: ${result.exception.message}", result.exception)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve descriptions: ${result.exception.message}", result.exception)
            }
            else -> {
                logger.error("Unexpected result retrieving descriptions: $result")
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve descriptions")
            }
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
        // First check if description exists using ServiceResult
        when (val findResult = standardizedDescriptionService.findByDescriptionNameStandardized(descriptionName)) {
            is ServiceResult.Success -> {
                // Description exists, proceed with update
                when (val updateResult = standardizedDescriptionService.update(toBePatchedDescription)) {
                    is ServiceResult.Success -> {
                        logger.info("Description updated successfully: $descriptionName (legacy endpoint)")
                        return ResponseEntity.ok(updateResult.data)
                    }
                    is ServiceResult.NotFound -> {
                        logger.warn("Description not found for update: $descriptionName")
                        throw ResponseStatusException(HttpStatus.NOT_FOUND, "Description not found: $descriptionName")
                    }
                    is ServiceResult.ValidationError -> {
                        logger.error("Validation error updating description $descriptionName: ${updateResult.errors}")
                        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Validation error: ${updateResult.errors}")
                    }
                    is ServiceResult.BusinessError -> {
                        logger.error("Business error updating description $descriptionName: ${updateResult.message}")
                        throw ResponseStatusException(HttpStatus.CONFLICT, updateResult.message)
                    }
                    is ServiceResult.SystemError -> {
                        logger.error("Failed to update description $descriptionName: ${updateResult.exception.message}", updateResult.exception)
                        throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update description: ${updateResult.exception.message}", updateResult.exception)
                    }
                    else -> {
                        logger.error("Unexpected update result: $updateResult")
                        throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update description")
                    }
                }
            }
            is ServiceResult.NotFound -> {
                logger.warn("Description not found for update: $descriptionName")
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Description not found: $descriptionName")
            }
            is ServiceResult.SystemError -> {
                logger.error("Failed to find description $descriptionName: ${findResult.exception.message}", findResult.exception)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to find description: ${findResult.exception.message}", findResult.exception)
            }
            else -> {
                logger.error("Unexpected find result: $findResult")
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to find description")
            }
        }
    }

    /**
     * Legacy endpoint - GET /api/description/select/{description_name}
     * Maintains snake_case @PathVariable annotation for backward compatibility
     */
    @GetMapping("/select/{description_name}")
    fun selectDescriptionName(@PathVariable("description_name") descriptionName: String): ResponseEntity<Description> {
        return when (val result = standardizedDescriptionService.findByDescriptionNameStandardized(descriptionName)) {
            is ServiceResult.Success -> {
                logger.info("Retrieved description: $descriptionName (legacy endpoint)")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Description not found: $descriptionName")
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Description not found: $descriptionName")
            }
            is ServiceResult.SystemError -> {
                logger.error("Failed to retrieve description $descriptionName: ${result.exception.message}", result.exception)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve description: ${result.exception.message}", result.exception)
            }
            else -> {
                logger.error("Unexpected result retrieving description $descriptionName: $result")
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve description")
            }
        }
    }

    /**
     * Legacy endpoint - POST /api/description/insert
     */
    @PostMapping("/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insertDescription(@RequestBody description: Description): ResponseEntity<Description> {
        return when (val result = standardizedDescriptionService.save(description)) {
            is ServiceResult.Success -> {
                logger.info("Description inserted successfully: ${description.descriptionName} (legacy endpoint)")
                ResponseEntity(result.data, HttpStatus.CREATED)
            }
            is ServiceResult.ValidationError -> {
                logger.error("Validation error inserting description ${description.descriptionName}: ${result.errors}")
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Validation error: ${result.errors}")
            }
            is ServiceResult.BusinessError -> {
                logger.error("Failed to insert description due to business error: ${result.message}")
                if (result.errorCode == "DATA_INTEGRITY_VIOLATION") {
                    throw ResponseStatusException(HttpStatus.CONFLICT, "Duplicate description found.")
                } else {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, result.message)
                }
            }
            is ServiceResult.SystemError -> {
                logger.error("Unexpected error inserting description ${description.descriptionName}: ${result.exception.message}", result.exception)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: ${result.exception.message}", result.exception)
            }
            else -> {
                logger.error("Unexpected result inserting description ${description.descriptionName}: $result")
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error")
            }
        }
    }

    /**
     * Legacy endpoint - DELETE /api/description/delete/{descriptionName}
     */
    @DeleteMapping("/delete/{descriptionName}", produces = ["application/json"])
    fun deleteByDescription(@PathVariable descriptionName: String): ResponseEntity<Description> {
        // First find the description to return it
        return when (val findResult = standardizedDescriptionService.findByDescriptionNameStandardized(descriptionName)) {
            is ServiceResult.Success -> {
                val description = findResult.data
                when (val deleteResult = standardizedDescriptionService.deleteByDescriptionNameStandardized(descriptionName)) {
                    is ServiceResult.Success -> {
                        logger.info("Description deleted successfully: $descriptionName (legacy endpoint)")
                        ResponseEntity.ok(description)
                    }
                    is ServiceResult.NotFound -> {
                        logger.warn("Description not found for deletion: $descriptionName")
                        throw ResponseStatusException(HttpStatus.NOT_FOUND, "Description not found: $descriptionName")
                    }
                    is ServiceResult.SystemError -> {
                        logger.error("Failed to delete description $descriptionName: ${deleteResult.exception.message}", deleteResult.exception)
                        throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete description: ${deleteResult.exception.message}", deleteResult.exception)
                    }
                    else -> {
                        logger.error("Unexpected delete result: $deleteResult")
                        throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete description")
                    }
                }
            }
            is ServiceResult.NotFound -> {
                logger.warn("Description not found for deletion: $descriptionName")
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Description not found: $descriptionName")
            }
            is ServiceResult.SystemError -> {
                logger.error("Failed to find description $descriptionName: ${findResult.exception.message}", findResult.exception)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to find description: ${findResult.exception.message}", findResult.exception)
            }
            else -> {
                logger.error("Unexpected find result: $findResult")
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to find description")
            }
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
            val merged = standardizedDescriptionService.mergeDescriptions(request.targetName, request.sourceNames)
            ResponseEntity.ok(merged)
        } catch (ex: ResponseStatusException) {
            throw ex
        } catch (ex: Exception) {
            logger.error("Failed to merge descriptions into ${request.targetName}: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to merge descriptions: ${ex.message}", ex)
        }
    }
}
