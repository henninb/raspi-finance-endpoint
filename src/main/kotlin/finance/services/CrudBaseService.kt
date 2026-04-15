package finance.services

import finance.configurations.ResilienceComponents
import finance.domain.DomainException
import finance.domain.ServiceResult
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validator
import org.springframework.dao.DataIntegrityViolationException

/**
 * Enhanced service base class with standardized patterns
 * Provides consistent error handling and response patterns for all CRUD operations
 *
 * @param T The entity type
 * @param ID The entity identifier type
 */
abstract class CrudBaseService<T, ID>
    constructor(
        meterService: MeterService,
        validator: Validator,
        resilienceComponents: ResilienceComponents,
    ) : BaseService(meterService, validator, resilienceComponents),
        StandardServiceInterface<T, ID> {
        /**
         * Abstract method to get entity name for logging and error messages
         * @return The entity name (e.g., "Account", "Transaction", "Parameter")
         */
        protected abstract fun getEntityName(): String

        /**
         * Standardized error handling wrapper for service operations
         * Converts various exceptions into appropriate ServiceResult types
         *
         * @param operation Description of the operation for logging
         * @param entityId The entity identifier (can be null for operations like findAll)
         * @param block The operation to execute
         * @return ServiceResult with appropriate success or error type
         */
        protected fun <R> handleServiceOperation(
            operation: String,
            entityId: ID?,
            block: () -> R,
        ): ServiceResult<R> =
            try {
                logger.debug("$operation ${getEntityName()}: $entityId")
                val result = block()
                logger.info("Successfully completed $operation for ${getEntityName()}: $entityId")
                ServiceResult.Success.of(result)
            } catch (ex: EntityNotFoundException) {
                val message = "${getEntityName()} not found: $entityId"
                logger.warn(message, ex)
                ServiceResult.NotFound.of(message)
            } catch (ex: ConstraintViolationException) {
                val message = "Validation error in $operation for ${getEntityName()}: ${ex.message}"
                logger.error(message, ex)
                val validationErrors = extractValidationErrors(ex)
                ServiceResult.ValidationError.of(validationErrors)
            } catch (ex: DataIntegrityViolationException) {
                val message = "Data integrity violation in $operation for ${getEntityName()}: ${ex.message}"
                logger.error(message, ex)
                ServiceResult.BusinessError.of(message, "DATA_INTEGRITY_VIOLATION")
            } catch (ex: IllegalStateException) {
                // Handle business logic state exceptions
                val message = "Business logic error in $operation for ${getEntityName()}: ${ex.message}"
                logger.error(message, ex)
                meterService.incrementExceptionThrownCounter("BusinessLogicError")
                ServiceResult.BusinessError.of(message, "BUSINESS_LOGIC_ERROR")
            } catch (ex: DomainException) {
                val message = "Business rule violated in $operation for ${getEntityName()}: ${ex.message}"
                logger.warn(message)
                meterService.incrementExceptionThrownCounter("DomainRuleViolation")
                ServiceResult.BusinessError.of(message, "DOMAIN_RULE_VIOLATION")
            } catch (ex: RuntimeException) {
                // Handle system-level exceptions (database failures, etc.)
                val message = "System error in $operation for ${getEntityName()}: ${ex.message}"
                logger.error(message, ex)
                meterService.incrementExceptionThrownCounter("SystemError")
                ServiceResult.SystemError.of(ex)
            } catch (ex: Exception) {
                // Handle other business logic exceptions
                val message = "Business logic error in $operation for ${getEntityName()}: ${ex.message}"
                logger.error(message, ex)
                meterService.incrementExceptionThrownCounter("BusinessLogicError")
                ServiceResult.BusinessError.of(message, "BUSINESS_LOGIC_ERROR")
            }

        /**
         * Extracts validation errors from ConstraintViolationException into a map
         * @param ex The constraint violation exception
         * @return Map of field names to error messages
         */
        private fun extractValidationErrors(ex: ConstraintViolationException): Map<String, String> {
            val errors =
                ex.constraintViolations.associate { violation ->
                    (violation.propertyPath?.toString() ?: "unknown") to (violation.message ?: "Validation failed")
                }
            return errors.ifEmpty { mapOf("validation" to "Validation failed") }
        }
    }
