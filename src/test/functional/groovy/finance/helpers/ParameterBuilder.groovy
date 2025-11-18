package finance.helpers

import finance.domain.Parameter

class ParameterBuilder {

    private String parameterName
    private String parameterValue
    private boolean activeStatus = true

    static ParameterBuilder builder() {
        new ParameterBuilder()
    }

    ParameterBuilder withParameterName(String parameterName) {
        this.parameterName = parameterName
        this
    }

    ParameterBuilder withParameterValue(String parameterValue) {
        this.parameterValue = parameterValue
        this
    }

    ParameterBuilder withActiveStatus(boolean activeStatus) {
        this.activeStatus = activeStatus
        this
    }

    Parameter build() {
        new Parameter(0L, parameterName, parameterValue, activeStatus)
    }

    Parameter buildAndValidate() {
        build()
    }
}
