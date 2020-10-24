package finance.controllers

import finance.domain.Parm
import finance.services.ParmService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.*
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException
import java.util.*
import javax.validation.ConstraintViolationException

@CrossOrigin
@RestController
@RequestMapping("/parm")
class ParmController(private var parmService: ParmService) {
    private val logger = LoggerFactory.getLogger(this.javaClass)
    
    //https://hornsup:8080/parm/select/payment_account
    @GetMapping(path = ["/select/{parmName}"], produces = ["application/json"])
    fun selectParm(@PathVariable parmName: String): ResponseEntity<Parm> {
        val parmOptional: Optional<Parm> = parmService.findByParm(parmName)
        if (!parmOptional.isPresent) {
            logger.info("no parm found.")
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "could not find the parm.")
        }
        return ResponseEntity.ok(parmOptional.get())
    }

    //curl --header "Content-Type: application/json" -X POST -d '{"parm":"test"}' http://localhost:8080/parm/insert
    @PostMapping(path = ["/insert"], produces = ["application/json"])
    fun insertParm(@RequestBody parm: Parm): ResponseEntity<String> {
        parmService.insertParm(parm)
        logger.info("insertParm")
        return ResponseEntity.ok("parm inserted")
    }

//    @DeleteMapping(path = ["/delete/{parmName}"], produces = ["application/json"])
//    fun deleteByParmName(@PathVariable parmName: String): ResponseEntity<String> {
//        parmService.deleteByParmName(parmName)
//        return ResponseEntity.ok("payment deleted")
//    }

    @ResponseStatus(HttpStatus.BAD_REQUEST) //400
    @ExceptionHandler(value = [ConstraintViolationException::class, NumberFormatException::class, MethodArgumentTypeMismatchException::class, HttpMessageNotReadableException::class])
    fun handleBadHttpRequests(throwable: Throwable): Map<String, String>? {
        val response: MutableMap<String, String> = HashMap()
        logger.error("Bad Request", throwable)
        response["response"] = "BAD_REQUEST: " + throwable.javaClass.simpleName + " , message: " + throwable.message
        return response
    }
}
