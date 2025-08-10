package finance.controllers

import finance.domain.Parameter
import finance.services.ParameterService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.*

@CrossOrigin
@RestController
//TODO: need to change to parameter
@RequestMapping("/parm", "/api/parameter", "/api/parm", "/parameter")
class ParameterController(private val parameterService: ParameterService) : BaseController() {

    // curl -k https://localhost:8443/parameter/select/active
    @GetMapping("/select/active", produces = ["application/json"])
    fun parameters(): ResponseEntity<List<Parameter>> {
        return try {
            logger.debug("Retrieving active parameters")
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
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve parameters: ${ex.message}", ex)
        }
    }

    // curl -k https://localhost:8443/parameter/select/payment_account
    @GetMapping("/select/{parameterName}", produces = ["application/json"])
    fun selectParameter(@PathVariable parameterName: String): ResponseEntity<Parameter> {
        return try {
            logger.debug("Retrieving parameter: $parameterName")
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
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve parameter: ${ex.message}", ex)
        }
    }

    // curl -k --header "Content-Type: application/json" --request POST --data '{"parameterName":"test", "parameterValue":"test_value", "activeStatus": true}' https://localhost:8443/parameter/insert
    @PostMapping("/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insertParameter(@RequestBody parameter: Parameter): ResponseEntity<Parameter> {
        return try {
            logger.info("Inserting parameter: ${parameter.parameterName}")
            val parameterResponse = parameterService.insertParameter(parameter)
            logger.info("Parameter inserted successfully: ${parameterResponse.parameterName}")
            ResponseEntity.ok(parameterResponse)
        } catch (ex: ResponseStatusException) {
            logger.error("Failed to insert parameter ${parameter.parameterName}: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to insert parameter: ${ex.message}", ex)
        } catch (ex: jakarta.validation.ValidationException) {
            logger.error("Validation error inserting parameter ${parameter.parameterName}: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Validation error: ${ex.message}", ex)
        } catch (ex: Exception) {
            logger.error("Unexpected error inserting parameter ${parameter.parameterName}: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: ${ex.message}", ex)
        }
    }

    // curl -k --header "Content-Type: application/json" --request PUT --data '{"parameterName":"test", "parameterValue":"updated_value", "activeStatus": true}' https://localhost:8443/parameter/update/test
    @PutMapping("/update/{parameter_name}", consumes = ["application/json"], produces = ["application/json"])
    fun updateParameter(
        @PathVariable("parameter_name") parameterName: String,
        @RequestBody toBePatchedParameter: Parameter
    ): ResponseEntity<Parameter> {
        return try {
            logger.info("Updating parameter: $parameterName")
            parameterService.findByParameterName(parameterName)
                .orElseThrow {
                    logger.warn("Parameter not found for update: $parameterName")
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Parameter not found: $parameterName")
                }
            val parameterResponse = parameterService.updateParameter(toBePatchedParameter)
            logger.info("Parameter updated successfully: $parameterName")
            ResponseEntity.ok(parameterResponse)
        } catch (ex: ResponseStatusException) {
            throw ex
        } catch (ex: Exception) {
            logger.error("Failed to update parameter $parameterName: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update parameter: ${ex.message}", ex)
        }
    }

    // curl -k --header "Content-Type: application/json" --request DELETE https://localhost:8443/parameter/delete/test
    @DeleteMapping("/delete/{parameterName}", produces = ["application/json"])
    fun deleteByParameterName(@PathVariable parameterName: String): ResponseEntity<Parameter> {
        return try {
            logger.info("Attempting to delete parameter: $parameterName")
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
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete parameter: ${ex.message}", ex)
        }
    }
}
