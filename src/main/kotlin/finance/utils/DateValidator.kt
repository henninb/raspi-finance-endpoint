package finance.utils

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.apache.logging.log4j.LogManager
import java.time.LocalDate

class DateValidator : ConstraintValidator<ValidDate, LocalDate?> {
    override fun initialize(constraintAnnotation: ValidDate) {
    }

    override fun isValid(
        value: LocalDate?,
        context: ConstraintValidatorContext,
    ): Boolean {
        logger.debug("dateToBeEvaluated: $value")
        if (value == null) return true
        val minDate = LocalDate.of(2000, 1, 1)
        val maxDate = LocalDate.now().plusYears(50)
        return !value.isBefore(minDate) && !value.isAfter(maxDate)
    }

    companion object {
        private val logger = LogManager.getLogger()
    }
}
