package finance.controllers

import finance.domain.Parameter
import finance.services.MeterService
import finance.services.ParameterService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.*

@CrossOrigin
@RestController
@RequestMapping("/parm")
class ParameterController(private var parameterService: ParameterService) : BaseController() {

    //https://hornsup:8080/parm/select/payment_account
    @GetMapping(path = ["/select/{parameterName}"], produces = ["application/json"])
    fun selectParameter(@PathVariable parameterName: String): ResponseEntity<Parameter> {
        val parameterOptional: Optional<Parameter> = parameterService.findByParameter(parameterName)
        if (!parameterOptional.isPresent) {
            logger.error("no parameter found.")
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "could not find the parm.")
        }
        return ResponseEntity.ok(parameterOptional.get())
    }

    //curl --header "Content-Type: application/json" -X POST -d '{"parm":"test"}' http://localhost:8080/parm/insert
    @PostMapping(path = ["/insert"], produces = ["application/json"])
    fun insertParameter(@RequestBody parameter: Parameter): ResponseEntity<String> {
        parameterService.insertParameter(parameter)
        logger.debug("insertParameter")
        return ResponseEntity.ok("parameter inserted")
    }

    @DeleteMapping(path = ["/delete/{parameterName}"], produces = ["application/json"])
    fun deleteByParameterName(@PathVariable parameterName: String): ResponseEntity<String> {
        parameterService.deleteByParameterName(parameterName)
        return ResponseEntity.ok("parameter deleted")
    }
}
