package finance.helpers

import finance.domain.Parameter

class ParameterBuilder {

    Long parameterId = 1
    String parameterName = 'payment_account'
    String parameterValue = 'bank_brian'
    Boolean activeStatus = true

    static ParameterBuilder builder() {
        return new ParameterBuilder()
    }

    Parameter build() {
        Parameter parameter = new Parameter().with {
            parameterId = this.parameterId
            parameterName = this.parameterName
            parameterValue = this.parameterValue
            activeStatus = this.activeStatus
            return it
        }
        return parameter
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

    ParameterBuilder withParameterId(Long parameterId) {
        this.parameterId = parameterId
        return this
    }
}
