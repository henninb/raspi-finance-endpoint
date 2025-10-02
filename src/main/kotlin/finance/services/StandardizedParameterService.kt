package finance.services

import finance.domain.Parameter
import finance.domain.ServiceResult
import finance.repositories.ParameterRepository
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

/**
 * Standardized Parameter Service implementing ServiceResult pattern
 * Provides both new standardized methods and legacy compatibility
 */
@Service
@Primary
class StandardizedParameterService(
    private val parameterRepository: ParameterRepository,
) : StandardizedBaseService<Parameter, Long>() {
    override fun getEntityName(): String = "Parameter"

    // ===== New Standardized ServiceResult Methods =====

    override fun findAllActive(): ServiceResult<List<Parameter>> =
        handleServiceOperation("findAllActive", null) {
            parameterRepository.findByActiveStatusIsTrue()
        }

    override fun findById(id: Long): ServiceResult<Parameter> =
        handleServiceOperation("findById", id) {
            val optionalParameter = parameterRepository.findById(id)
            if (optionalParameter.isPresent) {
                optionalParameter.get()
            } else {
                throw jakarta.persistence.EntityNotFoundException("Parameter not found: $id")
            }
        }

    override fun save(entity: Parameter): ServiceResult<Parameter> =
        handleServiceOperation("save", entity.parameterId) {
            val violations = validator.validate(entity)
            if (violations.isNotEmpty()) {
                throw jakarta.validation.ConstraintViolationException("Validation failed", violations)
            }
            parameterRepository.saveAndFlush(entity)
        }

    override fun update(entity: Parameter): ServiceResult<Parameter> =
        handleServiceOperation("update", entity.parameterId) {
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

    override fun deleteById(id: Long): ServiceResult<Boolean> =
        handleServiceOperation("deleteById", id) {
            val optionalParameter = parameterRepository.findById(id)
            if (optionalParameter.isEmpty) {
                throw jakarta.persistence.EntityNotFoundException("Parameter not found: $id")
            }
            parameterRepository.delete(optionalParameter.get())
            true
        }

    /**
     * ServiceResult version of findByParameterName for modern controller usage
     */
    fun findByParameterNameStandardized(parameterName: String): ServiceResult<Parameter> =
        handleServiceOperation("findByParameterName", null) {
            val optionalParameter = parameterRepository.findByParameterName(parameterName)
            if (optionalParameter.isPresent) {
                optionalParameter.get()
            } else {
                throw jakarta.persistence.EntityNotFoundException("Parameter not found: $parameterName")
            }
        }

    /**
     * ServiceResult version of deleteByParameterName for modern controller usage
     */
    fun deleteByParameterNameStandardized(parameterName: String): ServiceResult<Boolean> =
        handleServiceOperation("deleteByParameterName", null) {
            val optionalParameter = parameterRepository.findByParameterName(parameterName)
            if (optionalParameter.isEmpty) {
                throw jakarta.persistence.EntityNotFoundException("Parameter not found: $parameterName")
            }
            parameterRepository.delete(optionalParameter.get())
            true
        }
}
