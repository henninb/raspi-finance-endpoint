package finance.utils

import java.sql.Date
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class DateValidator : ConstraintValidator<ValidDate,Date> {
    override fun initialize(constraintAnnotation: ValidDate) {
    }

    override fun  isValid(value : Date , context: ConstraintValidatorContext ): Boolean {
        //greater than 1/1/2000
        println("dateToBeEvaluated: $value")
        return value > Date(946684800)
        //return value > Date(946684800000)
    }

}