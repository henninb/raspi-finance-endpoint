package finance.controllers

import finance.domain.Parameter
import finance.services.IParameterService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import jakarta.validation.Valid

@CrossOrigin
@RestController
@RequestMapping("/api/parameter")
class ParameterController(private val parameterService: IParameterService) :
    StandardizedBaseController(), StandardRestController<Parameter, String> {

    // ===== STANDARDIZED ENDPOINTS (NEW) =====

    /**
     * Standardized collection retrieval - GET /api/parameter/active
     * Returns empty list instead of throwing 404 (standardized behavior)
     */
    @GetMapping("/active", produces = ["application/json"])
    override fun findAllActive(): ResponseEntity<List<Parameter>> {
        return handleCrudOperation("Find all active parameters", null) {
            logger.debug("Retrieving all active parameters")
            val parameters: List<Parameter> = parameterService.selectAll()
            logger.info("Retrieved ${parameters.size} active parameters")
            parameters
        }
    }

    /**
     * Standardized single entity retrieval - GET /api/parameter/{parameterName}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @GetMapping("/{parameterName}", produces = ["application/json"])
    override fun findById(@PathVariable parameterName: String): ResponseEntity<Parameter> {
        return handleCrudOperation("Find parameter by name", parameterName) {
            logger.debug("Retrieving parameter: $parameterName")
            val parameter = parameterService.findByParameterName(parameterName)
                .orElseThrow {
                    logger.warn("Parameter not found: $parameterName")
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Parameter not found: $parameterName")
                }
            logger.info("Retrieved parameter: $parameterName")
            parameter
        }
    }

    /**
     * Standardized entity creation - POST /api/parameter
     * Returns 201 CREATED
     */
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    override fun save(@Valid @RequestBody parameter: Parameter): ResponseEntity<Parameter> {
        return handleCreateOperation("Parameter", parameter.parameterName) {
            logger.info("Creating parameter: ${parameter.parameterName}")
            val result = parameterService.insertParameter(parameter)
            logger.info("Parameter created successfully: ${parameter.parameterName}")
            result
        }
    }

    /**
     * Standardized entity update - PUT /api/parameter/{parameterName}
     * Uses camelCase parameter without @PathVariable annotation
     */
    @PutMapping("/{parameterName}", consumes = ["application/json"], produces = ["application/json"])
    override fun update(@PathVariable parameterName: String, @Valid @RequestBody parameter: Parameter): ResponseEntity<Parameter> {
        return handleCrudOperation("Update parameter", parameterName) {
            logger.info("Updating parameter: $parameterName")
            // Validate parameter exists first
            parameterService.findByParameterName(parameterName)
                .orElseThrow {
                    logger.warn("Parameter not found for update: $parameterName")
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Parameter not found: $parameterName")
                }
            val result = parameterService.updateParameter(parameter)
            logger.info("Parameter updated successfully: $parameterName")
            result
        }
    }

    /**
     * Standardized entity deletion - DELETE /api/parameter/{parameterName}
     * Returns 200 OK with deleted entity
     */
    @DeleteMapping("/{parameterName}", produces = ["application/json"])
    override fun deleteById(@PathVariable parameterName: String): ResponseEntity<Parameter> {
        return handleDeleteOperation(
            "Parameter",
            parameterName,
            { parameterService.findByParameterName(parameterName) },
            { parameterService.deleteByParameterName(parameterName) }
        )
    }

    // ===== LEGACY ENDPOINTS (BACKWARD COMPATIBILITY) =====

    /**
     * Legacy endpoint - GET /api/parameter/select/active
     * Maintains original behavior: throws 404 if empty
     */
    @GetMapping("/select/active", produces = ["application/json"])
    fun parameters(): ResponseEntity<List<Parameter>> {
        return try {
            logger.debug("Retrieving active parameters (legacy endpoint)")
            val parameters: List<Parameter> = parameterService.selectAll()
            if (parameters.isEmpty()) {
                logger.warn("No parameters found")
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "No parameters found")
            }
            logger.info("Retrieved ${parameters.size} active parameters")
            ResponseEntity.ok(parameters)
        } catch (ex: ResponseStatusException) {
            throw ex
        } catch (ex: Exception) {
            logger.error("Failed to retrieve parameters: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve parameters: ${ex.message}")
        }
    }

    /**
     * Legacy endpoint - GET /api/parameter/select/{parameterName}
     */
    @GetMapping("/select/{parameterName}", produces = ["application/json"])
    fun selectParameter(@PathVariable parameterName: String): ResponseEntity<Parameter> {
        return try {
            logger.debug("Retrieving parameter: $parameterName (legacy endpoint)")
            val parameter = parameterService.findByParameterName(parameterName)
                .orElseThrow {
                    logger.warn("Parameter not found: $parameterName")
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Parameter not found: $parameterName")
                }
            logger.info("Retrieved parameter: $parameterName")
            ResponseEntity.ok(parameter)
        } catch (ex: ResponseStatusException) {
            throw ex
        } catch (ex: Exception) {
            logger.error("Failed to retrieve parameter $parameterName: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve parameter: ${ex.message}")
        }
    }

    /**
     * Legacy endpoint - POST /api/parameter/insert
     */
    @PostMapping("/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insertParameter(@RequestBody parameter: Parameter): ResponseEntity<Parameter> {
        return try {
            logger.info("Inserting parameter: ${parameter.parameterName} (legacy endpoint)")
            val parameterResponse = parameterService.insertParameter(parameter)
            logger.info("Parameter inserted successfully: ${parameterResponse.parameterName}")
            ResponseEntity(parameterResponse, HttpStatus.CREATED)
        } catch (ex: org.springframework.dao.DataIntegrityViolationException) {
            logger.error("Failed to insert parameter due to data integrity violation: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.CONFLICT, "Duplicate parameter found")
        } catch (ex: ResponseStatusException) {
            throw ex
        } catch (ex: jakarta.validation.ValidationException) {
            logger.error("Validation error inserting parameter ${parameter.parameterName}: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Validation error: ${ex.message}")
        } catch (ex: Exception) {
            logger.error("Unexpected error inserting parameter ${parameter.parameterName}: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: ${ex.message}")
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
        return try {
            logger.info("Updating parameter: $parameterName (legacy endpoint)")
            // Validate parameter exists first and get its ID
            val existingParameter = parameterService.findByParameterName(parameterName)
                .orElseThrow {
                    logger.warn("Parameter not found for update: $parameterName")
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Parameter not found: $parameterName")
                }

            // Ensure the parameter name and ID match the existing parameter
            val updatedParameter = Parameter(
                parameterId = existingParameter.parameterId,
                parameterName = parameterName,
                parameterValue = toBePatchedParameter.parameterValue,
                activeStatus = toBePatchedParameter.activeStatus
            )

            val parameterResponse = parameterService.updateParameter(updatedParameter)
            logger.info("Parameter updated successfully: $parameterName")
            ResponseEntity.ok(parameterResponse)
        } catch (ex: ResponseStatusException) {
            throw ex
        } catch (ex: Exception) {
            logger.error("Failed to update parameter $parameterName: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update parameter: ${ex.message}")
        }
    }

    /**
     * Legacy endpoint - DELETE /api/parameter/delete/{parameterName}
     */
    @DeleteMapping("/delete/{parameterName}", produces = ["application/json"])
    fun deleteByParameterName(@PathVariable parameterName: String): ResponseEntity<Parameter> {
        return try {
            logger.info("Attempting to delete parameter: $parameterName (legacy endpoint)")
            val parameter = parameterService.findByParameterName(parameterName)
                .orElseThrow {
                    logger.warn("Parameter not found for deletion: $parameterName")
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Parameter not found: $parameterName")
                }

            parameterService.deleteByParameterName(parameterName)
            logger.info("Parameter deleted successfully: $parameterName")
            ResponseEntity.ok(parameter)
        } catch (ex: ResponseStatusException) {
            throw ex
        } catch (ex: Exception) {
            logger.error("Failed to delete parameter $parameterName: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete parameter: ${ex.message}")
        }
    }
}
