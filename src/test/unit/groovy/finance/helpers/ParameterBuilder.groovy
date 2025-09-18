package finance.helpers

import finance.domain.Parameter
import java.sql.Timestamp
import java.util.*

class ParameterBuilder {

    Long parameterId = 0L
    String parameterName = 'payment_account'
    String parameterValue = 'bank_brian'
    Boolean activeStatus = true
    Timestamp dateUpdated = new Timestamp(Calendar.getInstance().time.time)

    static ParameterBuilder builder() {
        return new ParameterBuilder()
    }

    Parameter build() {
        Parameter parameter = new Parameter().with {
            parameterId = this.parameterId
            parameterName = this.parameterName
            parameterValue = this.parameterValue
            activeStatus = this.activeStatus
            dateUpdated = this.dateUpdated
            return it
        }
        return parameter
    }

    ParameterBuilder withParameterId(Long parameterId) {
        this.parameterId = parameterId
        return this
    }

    ParameterBuilder withParameterName(String parameterName) {
        this.parameterName = parameterName
        return this
    }

    ParameterBuilder withParameterValue(String parameterValue) {
        this.parameterValue = parameterValue
        return this
    }

    ParameterBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        return this
    }

    ParameterBuilder withDateUpdated(Timestamp dateUpdated) {
        this.dateUpdated = dateUpdated
        return this
    }
}
