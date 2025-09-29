package finance.utils

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.apache.logging.log4j.LogManager
import java.sql.Timestamp

class TimestampValidator : ConstraintValidator<ValidTimestamp, Timestamp> {
    override fun initialize(constraintAnnotation: ValidTimestamp) {
    }

    override fun isValid(
        value: Timestamp,
        context: ConstraintValidatorContext,
    ): Boolean {
        logger.debug("timestampToBeEvaluated: $value")
        return value > Timestamp.valueOf("2001-01-01") // Timestamp(946684800000)
    }

    companion object {
        private val logger = LogManager.getLogger()
    }
}
