package finance.helpers

import finance.domain.Parameter

class ParameterBuilder {

    String parameterName = 'foo'
    String parameterValue = 'bar'

    static ParameterBuilder builder() {
        return new ParameterBuilder()
    }

    Parameter build() {
        Parameter parameter = new Parameter()
        parameter.parameterName = parameterName
        parameter.parameterValue = parameterValue
        return parameter
    }

    ParameterBuilder parameterName(parameterName) {
        this.parameterName = parameterName
        return this
    }

    ParameterBuilder parameterValue(parameterValue) {
        this.parameterValue = parameterValue
        return this
    }
}
