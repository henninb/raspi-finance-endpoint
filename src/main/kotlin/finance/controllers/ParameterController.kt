package finance.controllers

import finance.domain.Parameter
import finance.domain.ServiceResult
import finance.services.StandardizedParameterService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import jakarta.validation.Valid

@CrossOrigin
@RestController
@RequestMapping("/api/parameter")
class ParameterController(private val standardizedParameterService: StandardizedParameterService) :
    StandardizedBaseController() {

    // ===== STANDARDIZED ENDPOINTS (NEW) =====

    /**
     * Standardized collection retrieval - GET /api/parameter/active
     * Returns empty list instead of throwing 404 (standardized behavior)
     * Uses ServiceResult pattern for enhanced error handling
     */
    @GetMapping("/active", produces = ["application/json"])
    fun findAllActive(): ResponseEntity<*> {
        return when (val result = standardizedParameterService.findAllActive()) {
            is ServiceResult.Success -> {
                logger.info("Retrieved ${result.data.size} active parameters")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("No parameters found")
                ResponseEntity.ok(emptyList<Parameter>())  // Standardized: return empty list, not 404
            }
            is ServiceResult.SystemError -> {
                logger.error("System error retrieving parameters: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("error" to "Internal server error"))
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("error" to "Internal server error"))
            }
        }
    }

    /**
     * Standardized single entity retrieval - GET /api/parameter/{parameterName}
     * Uses camelCase parameter and ServiceResult pattern for enhanced error handling
     */
    @GetMapping("/{parameterName}", produces = ["application/json"])
    fun findById(@PathVariable parameterName: String): ResponseEntity<*> {
        return when (val result = standardizedParameterService.findByParameterNameStandardized(parameterName)) {
            is ServiceResult.Success -> {
                logger.info("Retrieved parameter: $parameterName")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Parameter not found: $parameterName")
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(mapOf("error" to result.message))
            }
            is ServiceResult.SystemError -> {
                logger.error("System error retrieving parameter $parameterName: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("error" to "Internal server error"))
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("error" to "Internal server error"))
            }
        }
    }

    /**
     * Standardized entity creation - POST /api/parameter
     * Returns 201 CREATED with ServiceResult pattern for enhanced error handling
     */
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    fun save(@Valid @RequestBody parameter: Parameter): ResponseEntity<*> {
        return when (val result = standardizedParameterService.save(parameter)) {
            is ServiceResult.Success -> {
                logger.info("Parameter created successfully: ${parameter.parameterName}")
                ResponseEntity.status(HttpStatus.CREATED).body(result.data)
            }
            is ServiceResult.ValidationError -> {
                logger.warn("Validation error creating parameter: ${result.errors}")
                ResponseEntity.badRequest()
                    .body(mapOf("errors" to result.errors))
            }
            is ServiceResult.BusinessError -> {
                logger.warn("Business error creating parameter: ${result.message}")
                ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(mapOf("error" to result.message))
            }
            is ServiceResult.SystemError -> {
                logger.error("System error creating parameter: ${result.exception.message}", result.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("error" to "Internal server error"))
            }
            else -> {
                logger.error("Unexpected result type: $result")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("error" to "Internal server error"))
            }
        }
    }

    /**
     * Standardized entity update - PUT /api/parameter/{parameterName}
     * Uses camelCase parameter and ServiceResult pattern for enhanced error handling
     */
    @PutMapping("/{parameterName}", consumes = ["application/json"], produces = ["application/json"])
    fun update(@PathVariable parameterName: String, @Valid @RequestBody parameter: Parameter): ResponseEntity<*> {
        // First check if parameter exists
        return when (val existsResult = standardizedParameterService.findByParameterNameStandardized(parameterName)) {
            is ServiceResult.Success -> {
                // Parameter exists, proceed with update
                val existingParameter = existsResult.data
                val updatedParameter = Parameter(
                    parameterId = existingParameter.parameterId,
                    parameterName = parameterName,
                    parameterValue = parameter.parameterValue,
                    activeStatus = parameter.activeStatus
                )

                when (val updateResult = standardizedParameterService.update(updatedParameter)) {
                    is ServiceResult.Success -> {
                        logger.info("Parameter updated successfully: $parameterName")
                        ResponseEntity.ok(updateResult.data)
                    }
                    is ServiceResult.ValidationError -> {
                        logger.warn("Validation error updating parameter: ${updateResult.errors}")
                        ResponseEntity.badRequest()
                            .body(mapOf("errors" to updateResult.errors))
                    }
                    is ServiceResult.BusinessError -> {
                        logger.warn("Business error updating parameter: ${updateResult.message}")
                        ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(mapOf("error" to updateResult.message))
                    }
                    is ServiceResult.SystemError -> {
                        logger.error("System error updating parameter: ${updateResult.exception.message}", updateResult.exception)
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(mapOf("error" to "Internal server error"))
                    }
                    else -> {
                        logger.error("Unexpected result type: $updateResult")
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(mapOf("error" to "Internal server error"))
                    }
                }
            }
            is ServiceResult.NotFound -> {
                logger.warn("Parameter not found for update: $parameterName")
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(mapOf("error" to existsResult.message))
            }
            is ServiceResult.SystemError -> {
                logger.error("System error checking parameter existence: ${existsResult.exception.message}", existsResult.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("error" to "Internal server error"))
            }
            else -> {
                logger.error("Unexpected result type: $existsResult")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("error" to "Internal server error"))
            }
        }
    }

    /**
     * Standardized entity deletion - DELETE /api/parameter/{parameterName}
     * Returns 200 OK with deleted entity using ServiceResult pattern
     */
    @DeleteMapping("/{parameterName}", produces = ["application/json"])
    fun deleteById(@PathVariable parameterName: String): ResponseEntity<*> {
        // First get the parameter to return it after deletion
        return when (val findResult = standardizedParameterService.findByParameterNameStandardized(parameterName)) {
            is ServiceResult.Success -> {
                val parameterToDelete = findResult.data

                when (val deleteResult = standardizedParameterService.deleteByParameterNameStandardized(parameterName)) {
                    is ServiceResult.Success -> {
                        logger.info("Parameter deleted successfully: $parameterName")
                        ResponseEntity.ok(parameterToDelete)
                    }
                    is ServiceResult.SystemError -> {
                        logger.error("System error deleting parameter: ${deleteResult.exception.message}", deleteResult.exception)
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(mapOf("error" to "Internal server error"))
                    }
                    else -> {
                        logger.error("Unexpected result type: $deleteResult")
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(mapOf("error" to "Internal server error"))
                    }
                }
            }
            is ServiceResult.NotFound -> {
                logger.warn("Parameter not found for deletion: $parameterName")
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(mapOf("error" to findResult.message))
            }
            is ServiceResult.SystemError -> {
                logger.error("System error finding parameter for deletion: ${findResult.exception.message}", findResult.exception)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("error" to "Internal server error"))
            }
            else -> {
                logger.error("Unexpected result type: $findResult")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("error" to "Internal server error"))
            }
        }
    }

    // ===== LEGACY ENDPOINTS (BACKWARD COMPATIBILITY) =====

    /**
     * Legacy endpoint - GET /api/parameter/select/active
     * Maintains original behavior: throws 404 if empty
     */
    @GetMapping("/select/active", produces = ["application/json"])
    fun parameters(): ResponseEntity<List<Parameter>> {
        return when (val result = standardizedParameterService.findAllActive()) {
            is ServiceResult.Success -> {
                if (result.data.isEmpty()) {
                    logger.warn("No parameters found")
                    throw ResponseStatusException(HttpStatus.NOT_FOUND, "No parameters found")
                }
                logger.info("Retrieved ${result.data.size} active parameters")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("No parameters found")
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "No parameters found")
            }
            is ServiceResult.SystemError -> {
                logger.error("Failed to retrieve parameters: ${result.exception.message}", result.exception)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve parameters: ${result.exception.message}")
            }
            else -> {
                logger.error("Unexpected result type: $result")
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve parameters")
            }
        }
    }

    /**
     * Legacy endpoint - GET /api/parameter/select/{parameterName}
     */
    @GetMapping("/select/{parameterName}", produces = ["application/json"])
    fun selectParameter(@PathVariable parameterName: String): ResponseEntity<Parameter> {
        return when (val result = standardizedParameterService.findByParameterNameStandardized(parameterName)) {
            is ServiceResult.Success -> {
                logger.info("Retrieved parameter: $parameterName")
                ResponseEntity.ok(result.data)
            }
            is ServiceResult.NotFound -> {
                logger.warn("Parameter not found: $parameterName")
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Parameter not found: $parameterName")
            }
            is ServiceResult.SystemError -> {
                logger.error("Failed to retrieve parameter $parameterName: ${result.exception.message}", result.exception)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve parameter: ${result.exception.message}")
            }
            else -> {
                logger.error("Unexpected result type: $result")
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve parameter")
            }
        }
    }

    /**
     * Legacy endpoint - POST /api/parameter/insert
     */
    @PostMapping("/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insertParameter(@RequestBody parameter: Parameter): ResponseEntity<Parameter> {
        return when (val result = standardizedParameterService.save(parameter)) {
            is ServiceResult.Success -> {
                logger.info("Parameter inserted successfully: ${parameter.parameterName}")
                ResponseEntity(result.data, HttpStatus.CREATED)
            }
            is ServiceResult.ValidationError -> {
                logger.warn("Validation error inserting parameter: ${result.errors}")
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Validation error: ${result.errors}")
            }
            is ServiceResult.BusinessError -> {
                logger.error("Business error inserting parameter: ${result.message}")
                throw ResponseStatusException(HttpStatus.CONFLICT, "Duplicate parameter found")
            }
            is ServiceResult.SystemError -> {
                logger.error("System error inserting parameter: ${result.exception.message}", result.exception)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: ${result.exception.message}")
            }
            else -> {
                logger.error("Unexpected result type: $result")
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error")
            }
        }
    }

    /**
     * Legacy endpoint - PUT /api/parameter/update/{parameter_name}
     * Maintains snake_case @PathVariable annotation for backward compatibility
     */
    @PutMapping("/update/{parameter_name}", consumes = ["application/json"], produces = ["application/json"])
    fun updateParameter(
        @PathVariable("parameter_name") parameterName: String,
        @RequestBody toBePatchedParameter: Parameter
    ): ResponseEntity<Parameter> {
        // First check if parameter exists
        return when (val existsResult = standardizedParameterService.findByParameterNameStandardized(parameterName)) {
            is ServiceResult.Success -> {
                // Parameter exists, proceed with update
                val existingParameter = existsResult.data
                val updatedParameter = Parameter(
                    parameterId = existingParameter.parameterId,
                    parameterName = parameterName,
                    parameterValue = toBePatchedParameter.parameterValue,
                    activeStatus = toBePatchedParameter.activeStatus
                )

                when (val updateResult = standardizedParameterService.update(updatedParameter)) {
                    is ServiceResult.Success -> {
                        logger.info("Parameter updated successfully: $parameterName")
                        ResponseEntity.ok(updateResult.data)
                    }
                    is ServiceResult.ValidationError -> {
                        logger.warn("Validation error updating parameter: ${updateResult.errors}")
                        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Validation error: ${updateResult.errors}")
                    }
                    is ServiceResult.BusinessError -> {
                        logger.warn("Business error updating parameter: ${updateResult.message}")
                        throw ResponseStatusException(HttpStatus.CONFLICT, updateResult.message)
                    }
                    is ServiceResult.SystemError -> {
                        logger.error("System error updating parameter: ${updateResult.exception.message}", updateResult.exception)
                        throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update parameter: ${updateResult.exception.message}")
                    }
                    else -> {
                        logger.error("Unexpected result type: $updateResult")
                        throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update parameter")
                    }
                }
            }
            is ServiceResult.NotFound -> {
                logger.warn("Parameter not found for update: $parameterName")
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Parameter not found: $parameterName")
            }
            is ServiceResult.SystemError -> {
                logger.error("System error checking parameter existence: ${existsResult.exception.message}", existsResult.exception)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update parameter: ${existsResult.exception.message}")
            }
            else -> {
                logger.error("Unexpected result type: $existsResult")
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update parameter")
            }
        }
    }

    /**
     * Legacy endpoint - DELETE /api/parameter/delete/{parameterName}
     */
    @DeleteMapping("/delete/{parameterName}", produces = ["application/json"])
    fun deleteByParameterName(@PathVariable parameterName: String): ResponseEntity<Parameter> {
        // First get the parameter to return it after deletion
        return when (val findResult = standardizedParameterService.findByParameterNameStandardized(parameterName)) {
            is ServiceResult.Success -> {
                val parameterToDelete = findResult.data

                when (val deleteResult = standardizedParameterService.deleteByParameterNameStandardized(parameterName)) {
                    is ServiceResult.Success -> {
                        logger.info("Parameter deleted successfully: $parameterName")
                        ResponseEntity.ok(parameterToDelete)
                    }
                    is ServiceResult.SystemError -> {
                        logger.error("System error deleting parameter: ${deleteResult.exception.message}", deleteResult.exception)
                        throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete parameter: ${deleteResult.exception.message}")
                    }
                    else -> {
                        logger.error("Unexpected result type: $deleteResult")
                        throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete parameter")
                    }
                }
            }
            is ServiceResult.NotFound -> {
                logger.warn("Parameter not found for deletion: $parameterName")
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Parameter not found: $parameterName")
            }
            is ServiceResult.SystemError -> {
                logger.error("System error finding parameter for deletion: ${findResult.exception.message}", findResult.exception)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete parameter: ${findResult.exception.message}")
            }
            else -> {
                logger.error("Unexpected result type: $findResult")
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete parameter")
            }
        }
    }
}
