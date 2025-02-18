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
class ParameterController(private var parameterService: ParameterService) : BaseController() {

    @GetMapping("/select/active", produces = ["application/json"])
    fun parameters(): ResponseEntity<List<Parameter>> {

        val parameters: List<Parameter> = parameterService.selectAll()
        if (parameters.isEmpty()) {
            logger.info("no parameters found.")
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "could not find any parameters.")
        }
        logger.info("select active parameters: ${parameters.size}")
        return ResponseEntity.ok(parameters)
    }

    //https://hornsup:8443/paramameter/select/payment_account
    @GetMapping("/select/{parameterName}", produces = ["application/json"])
    fun selectParameter(@PathVariable parameterName: String): ResponseEntity<Parameter> {
        val parameterOptional: Optional<Parameter> = parameterService.findByParameterName(parameterName)
        if (!parameterOptional.isPresent) {
            logger.error("no parameter found.")
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "could not find the parameter.")
        }
        return ResponseEntity.ok(parameterOptional.get())
    }

    //curl --header "Content-Type: application/json" -X POST -d '{"parm":"test"}' http://localhost:8443/parameter/insert
    @PostMapping("/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insertParameter(@RequestBody parameter: Parameter): ResponseEntity<Parameter> {
        return try {
            val parameterResponse = parameterService.insertParameter(parameter)
            ResponseEntity.ok(parameterResponse)
        } catch (ex: ResponseStatusException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to insert parameter: ${ex.message}", ex)
        } catch (ex: Exception) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: ${ex.message}", ex)
        }
    }

    @PutMapping("/update/{parameter_name}", consumes = ["application/json"], produces = ["application/json"])
    fun updateParameter(
        @PathVariable("parameter_name") parameterName: String,
        @RequestBody toBePatchedParameter: Parameter
    ): ResponseEntity<Parameter> {
        val parameterOptional = parameterService.findByParameterName(parameterName)
        if (parameterOptional.isPresent) {
            val parameterResponse = parameterService.updateParameter(toBePatchedParameter)
            return ResponseEntity.ok(parameterResponse)
        }
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "Parameter not found for: $parameterName")
    }

    @DeleteMapping("/delete/{parameterName}", produces = ["application/json"])
    fun deleteByParameterName(@PathVariable parameterName: String): ResponseEntity<Parameter> {


        val parameterOptional: Optional<Parameter> = parameterService.findByParameterName(parameterName)

        if (parameterOptional.isPresent) {
            parameterService.deleteByParameterName(parameterName)
            val parameter = parameterOptional.get()
            logger.info("parameter deleted: ${parameter.parameterName}")
            return ResponseEntity.ok(parameter)
        }

        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "could not delete the description: $parameterName.")
    }
}
