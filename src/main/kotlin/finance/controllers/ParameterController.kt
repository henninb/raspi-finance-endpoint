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
@RequestMapping("/parm")
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

    //https://hornsup:8080/parm/select/payment_account
    @GetMapping("/select/{parameterName}", produces = ["application/json"])
    fun selectParameter(@PathVariable parameterName: String): ResponseEntity<Parameter> {
        val parameterOptional: Optional<Parameter> = parameterService.findByParameter(parameterName)
        if (!parameterOptional.isPresent) {
            logger.error("no parameter found.")
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "could not find the parameter.")
        }
        return ResponseEntity.ok(parameterOptional.get())
    }

    //curl --header "Content-Type: application/json" -X POST -d '{"parm":"test"}' http://localhost:8080/parm/insert
    @PostMapping("/insert", produces = ["application/json"])
    fun insertParameter(@RequestBody parameter: Parameter): ResponseEntity<Parameter> {
        val parameterResponse = parameterService.insertParameter(parameter)
        return ResponseEntity.ok(parameterResponse)
    }

    @DeleteMapping("/delete/{parameterName}", produces = ["application/json"])
    fun deleteByParameterName(@PathVariable parameterName: String): ResponseEntity<String> {
        parameterService.deleteByParameterName(parameterName)
        return ResponseEntity.ok("parameter deleted")
    }
}
