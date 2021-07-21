package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Parameter
import finance.repositories.ParameterRepository
import io.micrometer.core.annotation.Timed
import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.util.*
import javax.validation.ConstraintViolation
import javax.validation.ValidationException
import javax.validation.Validator

@Service
open class ParameterService(
    private var parameterRepository: ParameterRepository
) : IParameterService, BaseService() {

    @Timed
    override fun insertParameter(parameter: Parameter): Parameter {
        val constraintViolations: Set<ConstraintViolation<Parameter>> = validator.validate(parameter)
        handleConstraintViolations(constraintViolations, meterService)
        parameter.dateAdded = Timestamp(Calendar.getInstance().time.time)
        parameter.dateUpdated = Timestamp(Calendar.getInstance().time.time)
        return parameterRepository.saveAndFlush(parameter)
    }

    @Timed
    override fun deleteByParameterName(parameterName: String) : Boolean {
        parameterRepository.deleteByParameterName(parameterName)
        return true
    }

    @Timed
    override fun findByParameter(parameterName: String): Optional<Parameter> {
        val parameterOptional: Optional<Parameter> = parameterRepository.findByParameterName(parameterName)
        if (parameterOptional.isPresent) {
            return parameterOptional
        }
        return Optional.empty()
    }
}