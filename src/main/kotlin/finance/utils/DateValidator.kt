package finance.utils

import org.slf4j.LoggerFactory
import java.sql.Date
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class DateValidator : ConstraintValidator<ValidDate, Date> {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun initialize(constraintAnnotation: ValidDate) {
    }

    override fun isValid(value: Date, context: ConstraintValidatorContext): Boolean {
        logger.info("dateToBeEvaluated: $value")
        return value > Date(946684800)
    }
}