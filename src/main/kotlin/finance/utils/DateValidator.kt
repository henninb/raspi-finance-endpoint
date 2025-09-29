package finance.utils

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.apache.logging.log4j.LogManager
import java.sql.Date

class DateValidator : ConstraintValidator<ValidDate, Date?> {
    override fun initialize(constraintAnnotation: ValidDate) {
    }

    override fun isValid(
        value: Date?,
        context: ConstraintValidatorContext,
    ): Boolean {
        logger.debug("dateToBeEvaluated: $value")
        return value == null || value > Date.valueOf("2000-01-01")
    }

    companion object {
        private val logger = LogManager.getLogger()
    }
}
