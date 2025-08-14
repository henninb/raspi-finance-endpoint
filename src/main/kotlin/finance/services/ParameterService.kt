package finance.services

import finance.domain.Parameter
import finance.repositories.ParameterRepository
import io.micrometer.core.annotation.Timed
import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.sql.Timestamp
import java.util.*
import jakarta.validation.ConstraintViolation
import org.springframework.dao.DataIntegrityViolationException

@Service
open class ParameterService(
    private var parameterRepository: ParameterRepository
) : IParameterService, BaseService() {

    @Timed
    override fun selectAll(): List<Parameter> {
        val parameters = parameterRepository.findByActiveStatusIsTrue()

        if (parameters.isEmpty()) {
            logger.warn("selectAll - no parameters found.")
        } else {
            logger.info("selectAll - found parameters.")
        }
        return parameters
    }

    @Timed
    override fun insertParameter(parameter: Parameter): Parameter {
        logger.info("Attempting to insert parameter: ${mapper.writeValueAsString(parameter)}")

        val constraintViolations: Set<ConstraintViolation<Parameter>> = validator.validate(parameter)
        if (constraintViolations.isNotEmpty()) {
            logger.error("Validation failed for parameter: ${mapper.writeValueAsString(parameter)} - Violations: $constraintViolations")
            handleConstraintViolations(constraintViolations, meterService)
        }

        val timestamp = Timestamp(Calendar.getInstance().time.time)
        parameter.dateAdded = timestamp
        parameter.dateUpdated = timestamp

        return try {
            val savedParameter = parameterRepository.saveAndFlush(parameter)
            logger.info("Parameter inserted successfully: ${mapper.writeValueAsString(savedParameter)}")
            savedParameter
        } catch (ex: DataIntegrityViolationException) {
            logger.error("Database constraint violation while inserting parameter: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.CONFLICT, "Database constraint violation: ${ex.message}", ex)
        } catch (ex: Exception) {
            logger.error("Unexpected error while inserting parameter: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: ${ex.message}", ex)
        }
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
    override fun findByParameterName(parameterName: String): Optional<Parameter> {
        val parameterOptional: Optional<Parameter> = parameterRepository.findByParameterName(parameterName)
        if (parameterOptional.isPresent) {
            return parameterOptional
        }
        return Optional.empty()
    }

    @Transactional
    @Timed
    override fun updateParameter(parameter: Parameter): Parameter {
        val optionalParameter = parameterRepository.findByParameterId(parameter.parameterId)

        if (optionalParameter.isPresent) {
            val parameterToUpdate = optionalParameter.get()

            // Updating fields
            parameterToUpdate.parameterName = parameter.parameterName
            parameterToUpdate.parameterValue = parameter.parameterValue
            parameterToUpdate.activeStatus = parameter.activeStatus
            parameterToUpdate.dateUpdated = Timestamp(Calendar.getInstance().time.time)
            logger.info("parameter update")
            return parameterRepository.saveAndFlush(parameterToUpdate)
        }

        throw RuntimeException("Parameter not updated as the parameter does not exist: ${parameter.parameterId}.")
    }
}