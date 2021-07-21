package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.validation.ConstraintViolation
import javax.validation.ValidationException
import javax.validation.Validator

@Service
open class BaseService {
    @Autowired
    lateinit var meterService: MeterService

    @Autowired
    lateinit var validator: Validator

    fun handleConstraintViolations(constraintViolations: Set<ConstraintViolation<*>>, meterService: MeterService) {
        if (constraintViolations.isNotEmpty()) {
            var details = ""
            constraintViolations.forEach { constraintViolation ->
                details = constraintViolation.invalidValue.toString() + ": " + constraintViolation.message
                logger.error(details)
            }
            logger.error("Cannot insert record because of constraint violation(s): $details")
            meterService.incrementExceptionThrownCounter("ValidationException")
            throw ValidationException("Cannot insert record because of constraint violation(s): $details")
        }
    }

    companion object {
        val mapper = ObjectMapper()
        val logger: Logger = LogManager.getLogger()
    }
}