package finance.services

import finance.domain.Parameter
import finance.domain.ServiceResult
import finance.repositories.ParameterRepository
import jakarta.validation.ValidationException
import jakarta.validation.Validator
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus
import java.util.*

/**
 * Standardized Parameter Service implementing ServiceResult pattern
 * Provides both new standardized methods and legacy compatibility
 */
@Service
class StandardizedParameterService(
    private val parameterRepository: ParameterRepository
) : StandardizedBaseService<Parameter, Long>(), IParameterService {

    override fun getEntityName(): String = "Parameter"

    // ===== New Standardized ServiceResult Methods =====

    override fun findAllActive(): ServiceResult<List<Parameter>> {
        return handleServiceOperation("findAllActive", null) {
            parameterRepository.findByActiveStatusIsTrue()
        }
    }

    override fun findById(id: Long): ServiceResult<Parameter> {
        return handleServiceOperation("findById", id) {
            val optionalParameter = parameterRepository.findById(id)
            if (optionalParameter.isPresent) {
                optionalParameter.get()
            } else {
                throw jakarta.persistence.EntityNotFoundException("Parameter not found: $id")
            }
        }
    }

    override fun save(entity: Parameter): ServiceResult<Parameter> {
        return handleServiceOperation("save", entity.parameterId) {
            val violations = validator.validate(entity)
            if (violations.isNotEmpty()) {
                throw jakarta.validation.ConstraintViolationException("Validation failed", violations)
            }
            parameterRepository.saveAndFlush(entity)
        }
    }

    override fun update(entity: Parameter): ServiceResult<Parameter> {
        return handleServiceOperation("update", entity.parameterId) {
            val existingParameter = parameterRepository.findById(entity.parameterId!!)
            if (existingParameter.isEmpty) {
                throw jakarta.persistence.EntityNotFoundException("Parameter not found: ${entity.parameterId}")
            }

            // Update fields from the provided entity
            val parameterToUpdate = existingParameter.get()
            parameterToUpdate.parameterName = entity.parameterName
            parameterToUpdate.parameterValue = entity.parameterValue
            parameterToUpdate.activeStatus = entity.activeStatus
            parameterToUpdate.dateUpdated = entity.dateUpdated

            parameterRepository.saveAndFlush(parameterToUpdate)
        }
    }

    override fun deleteById(id: Long): ServiceResult<Boolean> {
        return handleServiceOperation("deleteById", id) {
            val optionalParameter = parameterRepository.findById(id)
            if (optionalParameter.isEmpty) {
                throw jakarta.persistence.EntityNotFoundException("Parameter not found: $id")
            }
            parameterRepository.delete(optionalParameter.get())
            true
        }
    }

    // ===== Legacy Method Compatibility =====

    override fun selectAll(): List<Parameter> {
        val result = findAllActive()
        return when (result) {
            is ServiceResult.Success -> result.data
            else -> emptyList()
        }
    }

    override fun insertParameter(parameter: Parameter): Parameter {
        val result = save(parameter)
        return when (result) {
            is ServiceResult.Success -> result.data
            is ServiceResult.ValidationError -> {
                val violations = result.errors.map { (field, message) ->
                    object : jakarta.validation.ConstraintViolation<Parameter> {
                        override fun getMessage(): String = message
                        override fun getMessageTemplate(): String = message
                        override fun getRootBean(): Parameter = parameter
                        override fun getRootBeanClass(): Class<Parameter> = Parameter::class.java
                        override fun getLeafBean(): Any = parameter
                        override fun getExecutableParameters(): Array<Any> = emptyArray()
                        override fun getExecutableReturnValue(): Any? = null
                        override fun getPropertyPath(): jakarta.validation.Path {
                            return object : jakarta.validation.Path {
                                override fun toString(): String = field
                                override fun iterator(): MutableIterator<jakarta.validation.Path.Node> = mutableListOf<jakarta.validation.Path.Node>().iterator()
                            }
                        }
                        override fun getInvalidValue(): Any? = null
                        override fun getConstraintDescriptor(): jakarta.validation.metadata.ConstraintDescriptor<*>? = null
                        override fun <U : Any?> unwrap(type: Class<U>?): U = throw UnsupportedOperationException()
                    }
                }.toSet()
                throw ValidationException(jakarta.validation.ConstraintViolationException("Validation failed", violations))
            }
            else -> throw RuntimeException("Failed to insert parameter: ${result}")
        }
    }

    override fun updateParameter(parameter: Parameter): Parameter {
        val result = update(parameter)
        return when (result) {
            is ServiceResult.Success -> result.data
            is ServiceResult.NotFound -> throw RuntimeException("Parameter not found: ${parameter.parameterId}")
            else -> throw RuntimeException("Failed to update parameter: ${result}")
        }
    }

    override fun findByParameterName(parameterName: String): Optional<Parameter> {
        return parameterRepository.findByParameterName(parameterName)
    }

    override fun deleteByParameterName(parameterName: String): Boolean {
        val optionalParameter = parameterRepository.findByParameterName(parameterName)
        if (optionalParameter.isEmpty) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Parameter not found: $parameterName")
        }
        parameterRepository.delete(optionalParameter.get())
        return true
    }
}