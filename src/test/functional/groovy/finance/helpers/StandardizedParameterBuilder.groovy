package finance.helpers

import finance.domain.Parameter

class StandardizedParameterBuilder {

    private String parameterName
    private String parameterValue
    private boolean activeStatus = true

    static StandardizedParameterBuilder builder() {
        new StandardizedParameterBuilder()
    }

    StandardizedParameterBuilder withParameterName(String parameterName) {
        this.parameterName = parameterName
        this
    }

    StandardizedParameterBuilder withParameterValue(String parameterValue) {
        this.parameterValue = parameterValue
        this
    }

    StandardizedParameterBuilder withActiveStatus(boolean activeStatus) {
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
