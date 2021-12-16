package finance.services

import finance.domain.Parameter
import finance.repositories.ParameterRepository
import io.micrometer.core.annotation.Timed
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.sql.Timestamp
import java.util.*
import javax.validation.ConstraintViolation

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
    override fun deleteByParameterName(parameterName: String): Boolean {
        val parameter = parameterRepository.findByParameterName(parameterName)
        if( parameter.isPresent) {
            parameterRepository.delete(parameter.get())
            return true
        }
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "category not found for: $parameterName")
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