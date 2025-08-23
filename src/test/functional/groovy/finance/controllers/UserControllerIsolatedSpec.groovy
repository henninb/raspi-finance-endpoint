package finance.controllers

import finance.domain.User
import finance.helpers.SmartUserBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared

@ActiveProfiles("func")
class UserControllerIsolatedSpec extends BaseControllerSpec {

    @Shared
    protected String endpointName = 'user'

    @Shared
    protected User testUser

    void setupSpec() {
        // Generate unique test user for this test run using SmartUserBuilder
        testUser = SmartUserBuilder.builderForOwner(testOwner)
            .withFirstName('functional')
            .withLastName('test')
            .withPassword('TestPass123!')
            .asActive()
            .buildAndValidate()
    }

    void 'should sign up new user successfully'() {
        given: "a new user signup payload"
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(testUser.toString(), headers)

        when: "posting to user signup endpoint"
        ResponseEntity<String> response = restTemplate.exchange(
            createURLWithPort("/api/user/signup"),
            HttpMethod.POST, entity, String)

        then: "response should be successful"
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'should reject duplicate user signup'() {
        given: "the same user signup payload as before"
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(testUser.toString(), headers)

        when: "posting to user signup endpoint again"
        ResponseEntity<String> response = restTemplate.exchange(
            createURLWithPort("/api/user/signup"),
            HttpMethod.POST, entity, String)

        then: "response should be conflict"
        response.statusCode == HttpStatus.CONFLICT
        0 * _
    }
}