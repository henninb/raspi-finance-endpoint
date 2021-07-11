package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.ValidationAmount
import finance.repositories.ValidationAmountRepository
import io.micrometer.core.annotation.Timed
import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Service
import java.util.*
import javax.validation.Validator
import javax.validation.ConstraintViolation
import javax.validation.ValidationException


@Service
open class ValidationAmountService(
    private var validationAmountRepository: ValidationAmountRepository,
    private val validator: Validator,
    private var meterService: MeterService
) : IValidationAmountService {

    companion object {
        private val mapper = ObjectMapper()
        private val logger = LogManager.getLogger()
    }

    override fun insertValidationAmount(validationAmount: ValidationAmount) : ValidationAmount {
        val constraintViolations: Set<ConstraintViolation<ValidationAmount>> = validator.validate(validationAmount)
        if (constraintViolations.isNotEmpty()) {
            constraintViolations.forEach { constraintViolation -> logger.error(constraintViolation.message) }
            logger.error("Cannot insert validationAmount as there is a constraint violation on the data.")
            meterService.incrementExceptionThrownCounter("ValidationException")
            throw ValidationException("Cannot insert validationAmount as there is a constraint violation on the data.")
        }
        return validationAmountRepository.saveAndFlush(validationAmount)
    }

    @Timed
    override fun findByAccountId(accountId: Long): ValidationAmount {
        val sortedList = validationAmountRepository.findByValidationId(accountId).sortedByDescending { it.validationDate }

        return sortedList.first()
    }
}
