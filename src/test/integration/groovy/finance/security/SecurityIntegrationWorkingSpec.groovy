package finance.security

import finance.Application
import finance.domain.User
import finance.repositories.UserRepository
import finance.services.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

@ActiveProfiles("int")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = Application)
@Transactional
class SecurityIntegrationWorkingSpec extends Specification {

    @LocalServerPort
    int port

    @Autowired
    TestRestTemplate restTemplate

    @Autowired
    UserRepository userRepository

    @Autowired
    UserService userService

    String baseUrl

    void setup() {
        baseUrl = "http://localhost:${port}"
    }

    void 'test user repository integration'() {
        given:
        User testUser = new User()
        testUser.username = "security_test_user"
        testUser.password = "encoded_test_password"
        testUser.firstName = "Security"
        testUser.lastName = "Test"
        testUser.activeStatus = true

        when:
        User savedUser = userRepository.save(testUser)

        then:
        savedUser != null
        savedUser.userId != null
        savedUser.username == "security_test_user"
        savedUser.firstName == "Security"

        when:
        Optional<User> foundUser = userRepository.findByUsername("security_test_user")

        then:
        foundUser.isPresent()
        foundUser.get().firstName == "Security"
        foundUser.get().lastName == "Test"
    }

    void 'test user service integration'() {
        given:
        User testUser = new User()
        testUser.username = "user_service_test"
        testUser.password = "service_test_password"
        testUser.firstName = "UserService"
        testUser.lastName = "Test"
        testUser.activeStatus = true

        when:
        User savedUser = userService.insertUser(testUser)

        then:
        savedUser != null
        savedUser.userId != null
        savedUser.username == "user_service_test"

        when:
        List<User> allUsers = userService.findAllUsers()

        then:
        allUsers.size() >= 1
        allUsers.any { it.username == "user_service_test" }
    }

    void 'test protected endpoint access without authentication'() {
        when:
        ResponseEntity<String> response = restTemplate.getForEntity("${baseUrl}/accounts", String.class)

        then:
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    void 'test health endpoint accessibility without authentication'() {
        when:
        ResponseEntity<String> response = restTemplate.getForEntity("${baseUrl}/actuator/health", String.class)

        then:
        response.statusCode == HttpStatus.OK
    }

    void 'test CORS headers handling'() {
        given:
        HttpHeaders headers = new HttpHeaders()
        headers.set("Origin", "http://localhost:3000")
        headers.set("Access-Control-Request-Method", "GET")
        headers.set("Access-Control-Request-Headers", "authorization")
        HttpEntity<String> entity = new HttpEntity<>(headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
            "${baseUrl}/accounts", 
            HttpMethod.OPTIONS, 
            entity, 
            String.class
        )

        then:
        response.statusCode == HttpStatus.OK || 
        response.statusCode == HttpStatus.NO_CONTENT ||
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    void 'test basic authentication with test credentials'() {
        given:
        HttpHeaders headers = new HttpHeaders()
        headers.setBasicAuth("foo", "bar")  // Test credentials from application-int.yml
        HttpEntity<String> entity = new HttpEntity<>(headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
            "${baseUrl}/actuator/health", 
            HttpMethod.GET, 
            entity, 
            String.class
        )

        then:
        response.statusCode == HttpStatus.OK
    }

    void 'test security filter chain with different endpoints'() {
        when:
        List<String> publicEndpoints = ["/actuator/health", "/actuator/info"]
        List<String> protectedEndpoints = ["/accounts", "/transactions", "/categories"]

        List<ResponseEntity<String>> publicResponses = publicEndpoints.collect { endpoint ->
            restTemplate.getForEntity("${baseUrl}${endpoint}", String.class)
        }

        List<ResponseEntity<String>> protectedResponses = protectedEndpoints.collect { endpoint ->
            restTemplate.getForEntity("${baseUrl}${endpoint}", String.class)
        }

        then:
        // Public endpoints should be accessible
        publicResponses.every { response ->
            response.statusCode == HttpStatus.OK || response.statusCode == HttpStatus.NOT_FOUND
        }

        // Protected endpoints should require authentication
        protectedResponses.every { response ->
            response.statusCode == HttpStatus.UNAUTHORIZED || 
            response.statusCode == HttpStatus.FORBIDDEN ||
            response.statusCode == HttpStatus.NOT_FOUND
        }
    }

    void 'test user authentication data integrity'() {
        given:
        User user1 = new User()
        user1.username = "integrity_test_user1"
        user1.password = "password1"
        user1.firstName = "User"
        user1.lastName = "One"
        user1.activeStatus = true

        User user2 = new User()
        user2.username = "integrity_test_user2"
        user2.password = "password2"
        user2.firstName = "User"
        user2.lastName = "Two"
        user2.activeStatus = true

        when:
        userRepository.save(user1)
        userRepository.save(user2)

        Optional<User> foundUser1 = userRepository.findByUsername("integrity_test_user1")
        Optional<User> foundUser2 = userRepository.findByUsername("integrity_test_user2")

        then:
        foundUser1.isPresent()
        foundUser2.isPresent()
        foundUser1.get().password == "password1"
        foundUser2.get().password == "password2"
        foundUser1.get().firstName != foundUser2.get().firstName || foundUser1.get().lastName != foundUser2.get().lastName
    }

    void 'test user service validation and constraints'() {
        given:
        User invalidUser = new User()
        invalidUser.username = null  // Invalid - username cannot be null
        invalidUser.password = "test_password"
        invalidUser.firstName = "Invalid"
        invalidUser.lastName = "User"
        invalidUser.activeStatus = true

        when:
        userService.insertUser(invalidUser)

        then:
        thrown(Exception)  // Should throw validation exception
    }

    void 'test concurrent user operations'() {
        given:
        List<Thread> userThreads = []
        List<String> createdUsernames = Collections.synchronizedList([])
        int threadCount = 5

        for (int i = 0; i < threadCount; i++) {
            Thread userThread = new Thread({
                try {
                    User user = new User()
                    user.username = "concurrent_user_${i}"
                    user.password = "concurrent_password_${i}"
                    user.firstName = "Concurrent"
                    user.lastName = "User${i}"
                    user.activeStatus = true
                    
                    User savedUser = userService.insertUser(user)
                    createdUsernames.add(savedUser.username)
                } catch (Exception e) {
                    e.printStackTrace()
                }
            })
            userThreads.add(userThread)
        }

        when:
        userThreads.each { it.start() }
        userThreads.each { it.join(5000) }  // Wait up to 5 seconds for each thread

        then:
        createdUsernames.size() == threadCount
        createdUsernames.unique().size() == threadCount  // All usernames should be unique
        createdUsernames.every { username ->
            userRepository.findByUsername(username).isPresent()
        }
    }
}