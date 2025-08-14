package finance.controllers

import finance.Application
import finance.domain.User
import finance.helpers.UserBuilder
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared
import spock.lang.Stepwise

@Stepwise
@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerSpec extends BaseControllerSpec {

    @Shared
    protected User user = UserBuilder.builder().withUsername('test_user_unique').build()

    @Shared
    protected String endpointName = 'user'

    void 'test sign up'() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(user.toString(), headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(createURLWithPort("/api/user/signup"),
                HttpMethod.POST, entity, String)

        then:
        response.statusCode == HttpStatus.OK
    }

    void 'test sign up - duplicate'() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(user.toString(), headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(createURLWithPort("/api/user/signup"),
                HttpMethod.POST, entity, String)

        then:
        response.statusCode == HttpStatus.CONFLICT
    }
}
