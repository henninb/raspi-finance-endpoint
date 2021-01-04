package finance.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Parameter
import finance.services.ParameterService
import org.apache.logging.log4j.LogManager
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.*
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException
import java.util.*
import javax.validation.ConstraintViolationException
import javax.validation.ValidationException

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
