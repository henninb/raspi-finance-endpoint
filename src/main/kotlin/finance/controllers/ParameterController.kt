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

    //https://hornsup:8443/parm/select/payment_account
    @GetMapping("/select/{parameterName}", produces = ["application/json"])
    fun selectParameter(@PathVariable parameterName: String): ResponseEntity<Parameter> {
        val parameterOptional: Optional<Parameter> = parameterService.findByParameter(parameterName)
        if (!parameterOptional.isPresent) {
            logger.error("no parameter found.")
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "could not find the parameter.")
        }
        return ResponseEntity.ok(parameterOptional.get())
    }

    //curl --header "Content-Type: application/json" -X POST -d '{"parm":"test"}' http://localhost:8443/parm/insert
    @PostMapping("/insert", produces = ["application/json"])
    fun insertParameter(@RequestBody parameter: Parameter): ResponseEntity<Parameter> {
        val parameterResponse = parameterService.insertParameter(parameter)
        return ResponseEntity.ok(parameterResponse)
    }

    @PutMapping("/update/{id}", consumes = ["application/json"], produces = ["application/json"])
    fun updateParameter(
        @PathVariable("parameterName") parameterName: String,
        @RequestBody toBePatchedParameter: Parameter
    ): ResponseEntity<Parameter> {
        val parameterResponse = parameterService.updateParameter(toBePatchedParameter)
        return ResponseEntity.ok(parameterResponse)
    }

    @DeleteMapping("/delete/{parameterName}", produces = ["application/json"])
    fun deleteByParameterName(@PathVariable parameterName: String): ResponseEntity<Parameter> {


        val parameterOptional: Optional<Parameter> = parameterService.findByParameter(parameterName)

        if (parameterOptional.isPresent) {
            parameterService.deleteByParameterName(parameterName)
            val parameter = parameterOptional.get()
            logger.info("parameter deleted: ${parameter.parameterName}")
            return ResponseEntity.ok(parameter)
        }

        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "could not delete the description: $parameterName.")
    }
}
