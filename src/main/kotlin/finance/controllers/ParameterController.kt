package finance.controllers

import finance.domain.Parameter
import finance.domain.ServiceResult
import finance.services.StandardizedParameterService
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

@CrossOrigin
@RestController
@RequestMapping("/api/parameter")
class ParameterController(
    private val standardizedParameterService: StandardizedParameterService,
) : StandardizedBaseController(),
    StandardRestController<Parameter, String> {
    // ===== STANDARDIZED ENDPOINTS (NEW) =====

    /**
     * Standardized collection retrieval - GET /api/parameter/active
     * Returns empty list instead of throwing 404 (standardized behavior)
     * Uses ServiceResult pattern for enhanced error handling
     */
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<Parameter>> =
        when (val result = standardizedParameterService.findAllActive()) {
            is ServiceResult.Success -> {
                logger.info("Retrieved ${result.data.size} active parameters")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("No parameters found")
                ResponseEntity.ok(emptyList<Parameter>()) // Standardized: return empty list, not 404
            }
            is ServiceResult.SystemError -> {
                logger.error("System error retrieving parameters: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }

    /**
     * Standardized single entity retrieval - GET /api/parameter/{parameterName}
     * Uses camelCase parameter and ServiceResult pattern for enhanced error handling
     */
    @GetMapping("/{parameterName}", produces = ["application/json"])
    override fun findById(
        @PathVariable("parameterName") id: String,
    ): ResponseEntity<Parameter> =
        when (val result = standardizedParameterService.findByParameterNameStandardized(id)) {
            is ServiceResult.Success -> {
                logger.info("Retrieved parameter: $id")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Parameter not found: $id")
                ResponseEntity.notFound().build()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error retrieving parameter $id: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }

    /**
     * Standardized entity creation - POST /api/parameter
     * Returns 201 CREATED with ServiceResult pattern for enhanced error handling
     */
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    override fun save(
        @Valid @RequestBody entity: Parameter,
    ): ResponseEntity<Parameter> =
        when (val result = standardizedParameterService.save(entity)) {
            is ServiceResult.Success -> {
                logger.info("Parameter created successfully: ${entity.parameterName}")
                ResponseEntity.status(HttpStatus.CREATED).body(result.data)
            }
            is ServiceResult.ValidationError -> {
                logger.warn("Validation error creating parameter: ${result.errors}")
                ResponseEntity.badRequest().build()
            }
            is ServiceResult.BusinessError -> {
                logger.warn("Business error creating parameter: ${result.message}")
                ResponseEntity.status(HttpStatus.CONFLICT).build()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error creating parameter: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }

    /**
     * Standardized entity update - PUT /api/parameter/{parameterName}
     * Uses camelCase parameter and ServiceResult pattern for enhanced error handling
     */
    @PutMapping("/{parameterName}", consumes = ["application/json"], produces = ["application/json"])
    override fun update(
        @PathVariable("parameterName") id: String,
        @Valid @RequestBody entity: Parameter,
    ): ResponseEntity<Parameter> {
        // First check if parameter exists
        return when (val existsResult = standardizedParameterService.findByParameterNameStandardized(id)) {
            is ServiceResult.Success -> {
                // Parameter exists, proceed with update
                val existingParameter = existsResult.data
                val updatedParameter =
                    Parameter(
                        parameterId = existingParameter.parameterId,
                        parameterName = id,
                        parameterValue = entity.parameterValue,
                        activeStatus = entity.activeStatus,
                    )

                when (val updateResult = standardizedParameterService.update(updatedParameter)) {
                    is ServiceResult.Success -> {
                        logger.info("Parameter updated successfully: $id")
                        ResponseEntity.ok(updateResult.data)
                    }
                    is ServiceResult.ValidationError -> {
                        logger.warn("Validation error updating parameter: ${updateResult.errors}")
                        ResponseEntity.badRequest().build()
                    }
                    is ServiceResult.BusinessError -> {
                        logger.warn("Business error updating parameter: ${updateResult.message}")
                        ResponseEntity.status(HttpStatus.CONFLICT).build()
                    }
                    is ServiceResult.SystemError -> {
                        logger.error("System error updating parameter: ${updateResult.exception.message}", updateResult.exception)
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                    }
                    else -> {
                        logger.error("Unexpected result type: $updateResult")
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                    }
                }
            }
            is ServiceResult.NotFound -> {
                logger.warn("Parameter not found for update: $id")
                ResponseEntity.notFound().build()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error checking parameter existence: ${existsResult.exception.message}", existsResult.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
            else -> {
                logger.error("Unexpected result type: $existsResult")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    /**
     * Standardized entity deletion - DELETE /api/parameter/{parameterName}
     * Returns 200 OK with deleted entity using ServiceResult pattern
     */
    @DeleteMapping("/{parameterName}", produces = ["application/json"])
    override fun deleteById(
        @PathVariable("parameterName") id: String,
    ): ResponseEntity<Parameter> {
        // First get the parameter to return it after deletion
        return when (val findResult = standardizedParameterService.findByParameterNameStandardized(id)) {
            is ServiceResult.Success -> {
                val parameterToDelete = findResult.data

                when (val deleteResult = standardizedParameterService.deleteByParameterNameStandardized(id)) {
                    is ServiceResult.Success -> {
                        logger.info("Parameter deleted successfully: $id")
                        ResponseEntity.ok(parameterToDelete)
                    }
                    is ServiceResult.SystemError -> {
                        logger.error("System error deleting parameter: ${deleteResult.exception.message}", deleteResult.exception)
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                    }
                    else -> {
                        logger.error("Unexpected result type: $deleteResult")
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                    }
                }
            }
            is ServiceResult.NotFound -> {
                logger.warn("Parameter not found for deletion: $id")
                ResponseEntity.notFound().build()
            }
            is ServiceResult.SystemError -> {
                logger.error("System error finding parameter for deletion: ${findResult.exception.message}", findResult.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
            else -> {
                logger.error("Unexpected result type: $findResult")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }
}
