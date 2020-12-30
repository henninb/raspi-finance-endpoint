package finance.services

import spock.lang.Specification

class ParameterServiceSpec extends BaseServiceSpec {
    ParameterService parameterService = new ParameterService(parameterRepositoryMock, meterServiceMock)

    void setup() {
    }
}
