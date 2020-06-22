package finance.utils

import java.sql.Timestamp
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class TimestampValidator : ConstraintValidator<ValidTimestamp,Timestamp> {
    override fun initialize(constraintAnnotation: ValidTimestamp) {
    }

    override fun  isValid(value : Timestamp , context: ConstraintValidatorContext ): Boolean {
        //greater than 1/1/2000
        return value > Timestamp(946684800000)
    }
}