package finance

import spock.lang.Shared
import org.springframework.http.*

class HealthEndpointSpec extends BaseRestTemplateIntegrationSpec {

    @Shared
    protected HttpHeaders headers = new HttpHeaders()

    void 'should return health status when health endpoint is accessed'() {
        when:
        ResponseEntity<String> response = getMgmtWithRetry("/actuator/health")

        then:
        response.statusCode.is2xxSuccessful()
        response.body.contains("UP")
    }

    void 'should return health status when actuator health endpoint is accessed'() {
        when:
        ResponseEntity<String> response = getMgmtWithRetry("/actuator/health")

        then:
        response.statusCode.is2xxSuccessful()
        response.body.contains("UP")
    }
}
