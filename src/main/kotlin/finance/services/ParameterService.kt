package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Parameter
import finance.repositories.ParameterRepository
import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.util.*
import javax.validation.ConstraintViolation
import javax.validation.ValidationException
import javax.validation.Validator

@Service
class ParameterService(
    private var parameterRepository: ParameterRepository,
    private val validator: Validator,
    private var meterService: MeterService
) : IParameterService {

    override fun insertParameter(parameter: Parameter): Boolean {
        val constraintViolations: Set<ConstraintViolation<Parameter>> = validator.validate(parameter)
        if (constraintViolations.isNotEmpty()) {
            constraintViolations.forEach { constraintViolation -> logger.error(constraintViolation.message) }
            logger.error("Cannot insert parameter as there is a constraint violation on the data.")
            meterService.incrementExceptionThrownCounter("ValidationException")
            throw ValidationException("Cannot insert parameter as there is a constraint violation on the data.")
        }

        parameter.dateAdded = Timestamp(Calendar.getInstance().time.time)
        parameter.dateUpdated = Timestamp(Calendar.getInstance().time.time)
        parameterRepository.saveAndFlush(parameter)
        return true
    }

    override fun deleteByParameterName(parameterName: String) {
        parameterRepository.deleteByParameterName(parameterName)
    }

    override fun findByParameter(parameterName: String): Optional<Parameter> {
        val parameterOptional: Optional<Parameter> = parameterRepository.findByParameterName(parameterName)
        if (parameterOptional.isPresent) {
            return parameterOptional
        }
        return Optional.empty()
    }

    companion object {
        private val mapper = ObjectMapper()
        private val logger = LogManager.getLogger()
    }
}