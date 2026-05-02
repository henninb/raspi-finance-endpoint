package finance.services

import finance.configurations.ResilienceComponents
import finance.domain.Parameter
import finance.domain.ServiceResult
import finance.repositories.ParameterRepository
import finance.utils.TenantContext
import finance.utils.orThrowNotFound
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
        resilienceComponents: ResilienceComponents,
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
                parameterRepository.findByOwnerAndParameterId(owner, id).orThrowNotFound("Parameter", id)
            }

        override fun save(entity: Parameter): ServiceResult<Parameter> =
            handleServiceOperation("save", entity.parameterId) {
                val owner = TenantContext.getCurrentOwner()
                entity.owner = owner

                validateOrThrow(entity)
                parameterRepository.saveAndFlush(entity)
            }

        override fun update(entity: Parameter): ServiceResult<Parameter> =
            handleServiceOperation("update", entity.parameterId) {
                val owner = TenantContext.getCurrentOwner()
                val parameterToUpdate =
                    parameterRepository
                        .findByOwnerAndParameterId(owner, entity.parameterId)
                        .orThrowNotFound("Parameter", entity.parameterId)
                parameterToUpdate.parameterName = entity.parameterName
                parameterToUpdate.parameterValue = entity.parameterValue
                parameterToUpdate.activeStatus = entity.activeStatus
                parameterToUpdate.dateUpdated = entity.dateUpdated
                parameterRepository.saveAndFlush(parameterToUpdate)
            }

        override fun deleteById(id: Long): ServiceResult<Parameter> =
            handleServiceOperation("deleteById", id) {
                val owner = TenantContext.getCurrentOwner()
                val parameter = parameterRepository.findByOwnerAndParameterId(owner, id).orThrowNotFound("Parameter", id)
                parameterRepository.delete(parameter)
                parameter
            }

        /**
         * ServiceResult version of findByParameterName for modern controller usage
         */
        fun findByParameterNameStandardized(parameterName: String): ServiceResult<Parameter> =
            handleServiceOperation("findByParameterName", null) {
                val owner = TenantContext.getCurrentOwner()
                parameterRepository.findByOwnerAndParameterName(owner, parameterName).orThrowNotFound("Parameter", parameterName)
            }

        fun deleteByParameterNameStandardized(parameterName: String): ServiceResult<Parameter> =
            handleServiceOperation("deleteByParameterName", null) {
                val owner = TenantContext.getCurrentOwner()
                val parameter =
                    parameterRepository
                        .findByOwnerAndParameterName(owner, parameterName)
                        .orThrowNotFound("Parameter", parameterName)
                parameterRepository.delete(parameter)
                parameter
            }
    }
