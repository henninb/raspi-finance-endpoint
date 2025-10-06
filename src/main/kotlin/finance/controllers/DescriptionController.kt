package finance.controllers

import finance.domain.Description
import finance.domain.MergeDescriptionsRequest
import finance.domain.ServiceResult
import finance.services.StandardizedDescriptionService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@CrossOrigin
@RestController
@RequestMapping("/api/description")
class DescriptionController(
    private val standardizedDescriptionService: StandardizedDescriptionService,
) : StandardizedBaseController(),
    StandardRestController<Description, String> {
    // ===== STANDARDIZED ENDPOINTS (NEW) =====

    /**
     * Standardized collection retrieval - GET /api/description/active
     * Returns empty list instead of throwing 404 (standardized behavior)
     */
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<Description>> =
        when (val result = standardizedDescriptionService.findAllActive()) {
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

    /**
     * Standardized single entity retrieval - GET /api/description/{descriptionName}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @GetMapping("/{descriptionName}", produces = ["application/json"])
    override fun findById(
        @PathVariable("descriptionName") id: String,
    ): ResponseEntity<Description> =
        when (val result = standardizedDescriptionService.findByDescriptionNameStandardized(id)) {
            is ServiceResult.Success -> {
                logger.info("Retrieved description: $id")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Description not found: $id")
                ResponseEntity.notFound().build()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error retrieving description $id: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }

    /**
     * Standardized entity creation - POST /api/description
     * Returns 201 CREATED
     */
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    override fun save(
        @Valid @RequestBody entity: Description,
    ): ResponseEntity<Description> =
        when (val result = standardizedDescriptionService.save(entity)) {
            is ServiceResult.Success -> {
                logger.info("Description created successfully: ${entity.descriptionName}")
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

    /**
     * Standardized entity update - PUT /api/description/{descriptionName}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @PutMapping("/{descriptionName}", consumes = ["application/json"], produces = ["application/json"])
    override fun update(
        @PathVariable("descriptionName") id: String,
        @Valid @RequestBody entity: Description,
    ): ResponseEntity<Description> =
        when (val result = standardizedDescriptionService.update(entity)) {
            is ServiceResult.Success -> {
                logger.info("Description updated successfully: $id")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Description not found for update: $id")
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
                logger.error("System error updating description $id: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Description>()
            }
        }

    /**
     * Standardized entity deletion - DELETE /api/description/{descriptionName}
     * Returns 200 OK with deleted entity
     */
    @DeleteMapping("/{descriptionName}", produces = ["application/json"])
    override fun deleteById(
        @PathVariable("descriptionName") id: String,
    ): ResponseEntity<Description> {
        // First find the description to return it
        return when (val findResult = standardizedDescriptionService.findByDescriptionNameStandardized(id)) {
            is ServiceResult.Success -> {
                val description = findResult.data
                when (val deleteResult = standardizedDescriptionService.deleteByDescriptionNameStandardized(id)) {
                    is ServiceResult.Success -> {
                        logger.info("Description deleted successfully: $id")
                        ResponseEntity.ok(description)
                    }
                    is ServiceResult.NotFound -> {
                        logger.warn("Description not found for deletion: $id")
                        ResponseEntity.notFound().build<Description>()
                    }
                    is ServiceResult.SystemError -> {
                        logger.error("System error deleting description $id: ${deleteResult.exception.message}", deleteResult.exception)
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Description>()
                    }
                    else -> {
                        logger.error("Unexpected delete result type: $deleteResult")
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                    }
                }
            }
            is ServiceResult.NotFound -> {
                logger.warn("Description not found for deletion: $id")
                ResponseEntity.notFound().build<Description>()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error finding description $id: ${findResult.exception.message}", findResult.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Description>()
            }
            else -> {
                logger.error("Unexpected find result type: $findResult")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    // ===== BUSINESS LOGIC ENDPOINTS (PRESERVED) =====

    /**
     * Business logic endpoint - POST /api/description/merge
     * Preserved as-is, not part of standardization
     */
    @PostMapping("/merge", consumes = ["application/json"], produces = ["application/json"])
    fun mergeDescriptions(
        @RequestBody request: MergeDescriptionsRequest,
    ): ResponseEntity<Description> =
        try {
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
