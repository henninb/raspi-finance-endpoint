package finance.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.User
import finance.helpers.SmartUserBuilder
import finance.helpers.UserTestContext
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared

@ActiveProfiles("func")
class UserControllerFunctionalSpec extends BaseControllerFunctionalSpec {

    @Shared
    protected String endpointName = 'user'

    @Shared
    protected UserTestContext userTestContext

    void setupSpec() {
        // Create user test context using TestFixtures pattern
        userTestContext = testFixtures.createUserTestContext(testOwner)
    }

    void cleanupSpec() {
        // Clean up all test data for this test owner
        userTestContext?.cleanup()
    }

    void 'should sign up new user successfully'() {
        given: "a new user signup payload for this specific test"
        User uniqueUser = userTestContext.createUniqueUser("signup")
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        // Build JSON manually since password has WRITE_ONLY access and is excluded from toString()
        Map userMap = [username: uniqueUser.username, password: uniqueUser.password, firstName: uniqueUser.firstName, lastName: uniqueUser.lastName, activeStatus: uniqueUser.activeStatus]
        String jsonPayload = new ObjectMapper().writeValueAsString(userMap)
        HttpEntity entity = new HttpEntity<>(jsonPayload, headers)

        when: "posting to user signup endpoint"
        ResponseEntity<String> response = restTemplate.exchange(
            createURLWithPort("/api/user/signup"),
            HttpMethod.POST, entity, String)

        then: "response should be successful"
        response.statusCode == HttpStatus.OK
        response.body.contains(uniqueUser.username)
        0 * _
    }

    void 'should reject duplicate user signup'() {
        given: "a user that will be created twice to test conflict handling"
        User duplicateUser = userTestContext.createUniqueUser("duplicate")
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        // Build JSON manually since password has WRITE_ONLY access and is excluded from toString()
        Map userMap = [username: duplicateUser.username, password: duplicateUser.password, firstName: duplicateUser.firstName, lastName: duplicateUser.lastName, activeStatus: duplicateUser.activeStatus]
        String jsonPayload = new ObjectMapper().writeValueAsString(userMap)
        HttpEntity entity = new HttpEntity<>(jsonPayload, headers)

        // First signup (should succeed)
        ResponseEntity<String> firstResponse = restTemplate.exchange(
            createURLWithPort("/api/user/signup"),
            HttpMethod.POST, entity, String)
        assert firstResponse.statusCode == HttpStatus.OK

        when: "posting the same user signup payload again"
        ResponseEntity<String> response = restTemplate.exchange(
            createURLWithPort("/api/user/signup"),
            HttpMethod.POST, entity, String)

        then: "response should be conflict"
        response.statusCode == HttpStatus.CONFLICT
        0 * _
    }
}