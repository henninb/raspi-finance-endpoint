package finance.utils

import org.slf4j.LoggerFactory
import java.sql.Timestamp
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class TimestampValidator : ConstraintValidator<ValidTimestamp,Timestamp> {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun initialize(constraintAnnotation: ValidTimestamp) {
    }

    override fun  isValid(value : Timestamp , context: ConstraintValidatorContext ): Boolean {
        //greater than 1/1/2000
        println("timestampToBeEvaluated: $value")
        logger.info("timestampToBeEvaluated: $value")
        return value > Timestamp(946684800000)
    }
}