package finance.services

import finance.configurations.ResilienceComponents
import finance.domain.Parameter
import finance.domain.ServiceResult
import finance.repositories.ParameterRepository
import finance.utils.TenantContext
import jakarta.validation.Validator
import org.springframework.stereotype.Service

/**
 * Standardized Parameter Service implementing ServiceResult pattern
 * Provides both new standardized methods and legacy compatibility
 */
@Service
class ParameterService
    constructor(
        private val parameterRepository: ParameterRepository,
        meterService: MeterService,
        validator: Validator,
        resilienceComponents: ResilienceComponents? = null,
    ) : CrudBaseService<Parameter, Long>(meterService, validator, resilienceComponents) {
        override fun getEntityName(): String = "Parameter"

        // ===== New Standardized ServiceResult Methods =====

        override fun findAllActive(): ServiceResult<List<Parameter>> =
            handleServiceOperation("findAllActive", null) {
                val owner = TenantContext.getCurrentOwner()
                parameterRepository.findByOwnerAndActiveStatusIsTrue(owner)
            }

        override fun findById(id: Long): ServiceResult<Parameter> =
            handleServiceOperation("findById", id) {
                val owner = TenantContext.getCurrentOwner()
                val optionalParameter = parameterRepository.findByOwnerAndParameterId(owner, id)
                if (optionalParameter.isPresent) {
                    optionalParameter.get()
                } else {
                    throw jakarta.persistence.EntityNotFoundException("Parameter not found: $id")
                }
            }

        override fun save(entity: Parameter): ServiceResult<Parameter> =
            handleServiceOperation("save", entity.parameterId) {
                val owner = TenantContext.getCurrentOwner()
                entity.owner = owner

                val violations = validator.validate(entity)
                if (violations.isNotEmpty()) {
                    throw jakarta.validation.ConstraintViolationException("Validation failed", violations)
                }
                parameterRepository.saveAndFlush(entity)
            }

        override fun update(entity: Parameter): ServiceResult<Parameter> =
            handleServiceOperation("update", entity.parameterId) {
                val owner = TenantContext.getCurrentOwner()
                val existingParameter = parameterRepository.findByOwnerAndParameterId(owner, entity.parameterId)
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

        override fun deleteById(id: Long): ServiceResult<Parameter> =
            handleServiceOperation("deleteById", id) {
                val owner = TenantContext.getCurrentOwner()
                val optionalParameter = parameterRepository.findByOwnerAndParameterId(owner, id)
                if (optionalParameter.isEmpty) {
                    throw jakarta.persistence.EntityNotFoundException("Parameter not found: $id")
                }
                val parameter = optionalParameter.get()
                parameterRepository.delete(parameter)
                parameter
            }

        /**
         * ServiceResult version of findByParameterName for modern controller usage
         */
        fun findByParameterNameStandardized(parameterName: String): ServiceResult<Parameter> =
            handleServiceOperation("findByParameterName", null) {
                val owner = TenantContext.getCurrentOwner()
                val optionalParameter = parameterRepository.findByOwnerAndParameterName(owner, parameterName)
                if (optionalParameter.isPresent) {
                    optionalParameter.get()
                } else {
                    throw jakarta.persistence.EntityNotFoundException("Parameter not found: $parameterName")
                }
            }

        /**
         * ServiceResult version of deleteByParameterName for modern controller usage
         */
        fun deleteByParameterNameStandardized(parameterName: String): ServiceResult<Parameter> =
            handleServiceOperation("deleteByParameterName", null) {
                val owner = TenantContext.getCurrentOwner()
                val optionalParameter = parameterRepository.findByOwnerAndParameterName(owner, parameterName)
                if (optionalParameter.isEmpty) {
                    throw jakarta.persistence.EntityNotFoundException("Parameter not found: $parameterName")
                }
                val parameter = optionalParameter.get()
                parameterRepository.delete(parameter)
                parameter
            }
    }
