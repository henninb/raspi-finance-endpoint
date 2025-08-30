package finance.repositories

import finance.Application
import finance.domain.User
import finance.helpers.SmartUserBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification
import jakarta.validation.ConstraintViolationException

@ActiveProfiles("func")
@DataJpaTest
@ContextConfiguration(classes = [Application])
class UserJpaSpec extends Specification {

    @Autowired
    protected UserRepository userRepository

    @Autowired
    protected TestEntityManager entityManager

    @Shared
    protected String testOwner = "jpa_${UUID.randomUUID().toString().replace('-', '')[0..7]}"

    void 'test User - valid insert'() {
        given:
        long beforeUser = userRepository.count()
        User user = SmartUserBuilder.builderForOwner(testOwner)
            .withUsername("testuser_${testOwner}")
            .withPassword("testpass123")
            .withFirstName("Test")
            .withLastName("User")
            .buildAndValidate()

        when:
        User result = entityManager.persist(user)

        then:
        userRepository.count() == beforeUser + 1
        result.username == user.username
        result.firstName == user.firstName
        result.lastName == user.lastName
    }

    void 'test User - duplicate username constraint'() {
        given:
        String duplicateUsername = "duplicateuser_${testOwner}"
        User user1 = SmartUserBuilder.builderForOwner(testOwner)
            .withUsername(duplicateUsername)
            .withPassword("pass1pass")
            .buildAndValidate()
        User user2 = SmartUserBuilder.builderForOwner(testOwner)
            .withUsername(duplicateUsername)
            .withPassword("pass2pass")
            .buildAndValidate()
        entityManager.persist(user1)

        when:
        entityManager.persist(user2)

        then:
        Exception ex = thrown()
        ex.message.contains('duplicate') || ex.message.contains('unique') || ex.message.contains('constraint')
    }

    void 'test User - find by username'() {
        given:
        String uniqueUsername = "finduser_${testOwner}"
        User user = SmartUserBuilder.builderForOwner(testOwner)
            .withUsername(uniqueUsername)
            .withPassword("findpass12")
            .buildAndValidate()
        entityManager.persist(user)

        when:
        Optional<User> foundUser = userRepository.findByUsername(uniqueUsername)

        then:
        foundUser.isPresent()
        foundUser.get().username == uniqueUsername
    }

    void 'test User - delete record'() {
        given:
        long beforeUser = userRepository.count()
        User user = SmartUserBuilder.builderForOwner(testOwner)
            .withUsername("deleteuser_${testOwner}")
            .withPassword("deletepass")
            .buildAndValidate()
        entityManager.persist(user)

        when:
        userRepository.delete(user)

        then:
        userRepository.count() == beforeUser
    }

    void 'test User - invalid username constraint'() {
        given:
        User user = SmartUserBuilder.builderForOwner(testOwner)
            .withUsername("")  // Empty username
            .withPassword("validpass")
            .build()  // Use build() instead of buildAndValidate() to allow constraint violation

        when:
        entityManager.persist(user)

        then:
        ConstraintViolationException ex = thrown()
        ex.message.toLowerCase().contains('username')
        0 * _
    }
}

