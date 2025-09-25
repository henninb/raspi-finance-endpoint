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
        new ParameterBuilder()
    }

    Parameter build() {
        Parameter parameter = new Parameter().with {
            parameterId = this.parameterId
            parameterName = this.parameterName
            parameterValue = this.parameterValue
            activeStatus = this.activeStatus
            dateUpdated = this.dateUpdated
            it
        }
        parameter
    }

    ParameterBuilder withParameterId(Long parameterId) {
        this.parameterId = parameterId
        this
    }

    ParameterBuilder withParameterName(String parameterName) {
        this.parameterName = parameterName
        this
    }

    ParameterBuilder withParameterValue(String parameterValue) {
        this.parameterValue = parameterValue
        this
    }

    ParameterBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        this
    }

    ParameterBuilder withDateUpdated(Timestamp dateUpdated) {
        this.dateUpdated = dateUpdated
        this
    }
}
