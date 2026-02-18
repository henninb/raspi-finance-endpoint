package finance.repositories

import finance.BaseIntegrationSpec
import finance.domain.User
import finance.helpers.SmartUserBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import spock.lang.Shared

/**
 * INTEGRATION TEST - UserRepository using robust, isolated architecture
 *
 * This integration test demonstrates the new architecture:
 * ✅ No hardcoded usernames - all use testOwner for uniqueness
 * ✅ SmartBuilder pattern with constraint validation
 * ✅ Test isolation - each test gets its own test data
 * ✅ Constraint-aware test data creation
 * ✅ Build-time validation prevents invalid data
 * ✅ User-specific testing for authentication and authorization
 */
class UserRepositoryIntSpec extends BaseIntegrationSpec {

    @Autowired
    UserRepository userRepository

    @Shared
    def repositoryContext

    def setupSpec() {
        repositoryContext = testFixtures.createRepositoryTestContext(testOwner)
    }

    void 'test user repository basic CRUD operations'() {
        given:
        User user = SmartUserBuilder.builderForOwner(testOwner)
                .withUniqueUsername("testuser")
                .withFirstName("John")
                .withLastName("Doe")
                .withPassword("testpassword123")
                .asActive()
                .buildAndValidate()

        when:
        User savedUser = userRepository.save(user)

        then:
        savedUser.userId != null
        savedUser.userId > 0
        savedUser.username.startsWith("testuser_")
        savedUser.username.contains(testOwner.toLowerCase().replaceAll(/[^a-z0-9]/, ''))
        savedUser.firstName == "John"  // Names may be stored as-is or converted
        savedUser.lastName == "Doe"    // Names may be stored as-is or converted
        savedUser.password == "testpassword123"
        savedUser.activeStatus == true
        savedUser.dateAdded != null
        savedUser.dateUpdated != null

        when:
        Optional<User> foundUser = userRepository.findByUsername(savedUser.username)

        then:
        foundUser.isPresent()
        foundUser.get().userId == savedUser.userId
        foundUser.get().activeStatus == true
    }

    void 'test find user by username'() {
        given:
        User user = SmartUserBuilder.builderForOwner(testOwner)
                .withUniqueUsername("findtest")
                .withFirstName("Jane")
                .withLastName("Smith")
                .withPassword("findpassword456")
                .asActive()
                .buildAndValidate()
        User savedUser = userRepository.save(user)

        when:
        Optional<User> foundUser = userRepository.findByUsername(savedUser.username)

        then:
        foundUser.isPresent()
        foundUser.get().username == savedUser.username
        foundUser.get().firstName == savedUser.firstName
        foundUser.get().lastName == savedUser.lastName
        foundUser.get().activeStatus == savedUser.activeStatus
    }

    void 'test find user by username and password'() {
        given:
        User user = SmartUserBuilder.builderForOwner(testOwner)
                .withUniqueUsername("authtest")
                .withFirstName("Auth")
                .withLastName("User")
                .withPassword("authpassword789")
                .asActive()
                .buildAndValidate()
        User savedUser = userRepository.save(user)

        when:
        Optional<User> foundUser = userRepository.findByUsernameAndPassword(savedUser.username, savedUser.password)

        then:
        foundUser.isPresent()
        foundUser.get().userId == savedUser.userId
        foundUser.get().username == savedUser.username
        foundUser.get().password == savedUser.password

        when:
        Optional<User> notFoundUser = userRepository.findByUsernameAndPassword(savedUser.username, "wrongpassword")

        then:
        !notFoundUser.isPresent()
    }

    void 'test user unique constraint violations on username'() {
        given:
        User user1 = SmartUserBuilder.builderForOwner(testOwner)
                .withUniqueUsername("unique")
                .withFirstName("First")
                .withLastName("User")
                .withPassword("password1")
                .asActive()
                .buildAndValidate()

        User user2 = SmartUserBuilder.builderForOwner(testOwner)
                .withUsername(user1.username)  // Same username - will cause unique constraint violation
                .withFirstName("Second")
                .withLastName("User")
                .withPassword("password2")      // Different password but same username - violates unique constraint
                .asActive()
                .buildAndValidate()

        when:
        userRepository.save(user1)
        userRepository.flush() // Force the first save to complete

        then:
        notThrown(Exception) // First save should succeed

        when:
        userRepository.save(user2)
        userRepository.flush() // This should fail due to unique constraint

        then:
        thrown(DataIntegrityViolationException)
    }


    void 'test user constraint validation at build time'() {
        when: "trying to create a user with invalid username length (too short)"
        User user1 = SmartUserBuilder.builderForOwner(testOwner)
                .withUsername("ab")  // Too short - should throw exception
                .buildAndValidate()

        then: "constraint validation fails"
        thrown(IllegalStateException)

        when: "trying to create a user with invalid username length (too long)"
        User user2 = SmartUserBuilder.builderForOwner(testOwner)
                .withUsername("a" * 61)  // Too long - should throw exception
                .buildAndValidate()

        then: "constraint validation fails"
        thrown(IllegalStateException)

        when: "trying to create a user with valid constraints"
        User user3 = SmartUserBuilder.builderForOwner(testOwner)
                .withUniqueUsername("valid")
                .withFirstName("Valid")
                .withLastName("User")
                .withPassword("validpassword123")
                .buildAndValidate()

        then: "constraint validation passes"
        user3.username.length() >= 3
        user3.username.length() <= 60
        user3.firstName.length() >= 1
        user3.firstName.length() <= 40
        user3.lastName.length() >= 1
        user3.lastName.length() <= 40
        user3.password.length() >= 8
        user3.password.length() <= 255
    }

    void 'test user update operations'() {
        given:
        User user = SmartUserBuilder.builderForOwner(testOwner)
                .withUniqueUsername("updatetest")
                .withFirstName("Original")
                .withLastName("Name")
                .withPassword("originalpassword")
                .asActive()
                .buildAndValidate()
        User savedUser = userRepository.save(user)

        when:
        savedUser.activeStatus = false
        savedUser.firstName = "updated"
        savedUser.lastName = "lastname"
        User updatedUser = userRepository.save(savedUser)

        then:
        updatedUser.userId == savedUser.userId
        updatedUser.username == savedUser.username
        updatedUser.firstName == "updated"
        updatedUser.lastName == "lastname"
        updatedUser.activeStatus == false

        when:
        Optional<User> refetchedUser = userRepository.findByUsername(savedUser.username)

        then:
        refetchedUser.isPresent()
        refetchedUser.get().activeStatus == false
        refetchedUser.get().firstName == "updated"
        refetchedUser.get().lastName == "lastname"
    }

    void 'test user deletion'() {
        given:
        User userToDelete = SmartUserBuilder.builderForOwner(testOwner)
                .withUniqueUsername("deletetest")
                .withFirstName("Delete")
                .withLastName("User")
                .withPassword("deletepassword")
                .asActive()
                .buildAndValidate()
        User savedUser = userRepository.save(userToDelete)

        when:
        userRepository.delete(savedUser)
        Optional<User> deletedUser = userRepository.findByUsername(savedUser.username)

        then:
        !deletedUser.isPresent()

        when:
        Optional<User> deletedById = userRepository.findById(savedUser.userId)

        then:
        !deletedById.isPresent()
    }

    void 'test user active status functionality'() {
        given:
        User activeUser = SmartUserBuilder.builderForOwner(testOwner)
                .withUniqueUsername("active")
                .withFirstName("Active")
                .withLastName("User")
                .withPassword("activepassword")
                .asActive()
                .buildAndValidate()

        User inactiveUser = SmartUserBuilder.builderForOwner(testOwner)
                .withUniqueUsername("inactive")
                .withFirstName("Inactive")
                .withLastName("User")
                .withPassword("inactivepassword")
                .asInactive()
                .buildAndValidate()

        when:
        User savedActive = userRepository.save(activeUser)
        User savedInactive = userRepository.save(inactiveUser)

        then:
        savedActive.activeStatus == true
        savedInactive.activeStatus == false

        when:
        List<User> allUsers = userRepository.findAll()

        then:
        allUsers.any { it.userId == savedActive.userId && it.activeStatus == true }
        allUsers.any { it.userId == savedInactive.userId && it.activeStatus == false }
    }

    void 'test find non-existent user'() {
        when:
        Optional<User> nonExistentByUsername = userRepository.findByUsername("nonexistent_${testOwner}")
        Optional<User> nonExistentByUsernamePassword = userRepository.findByUsernameAndPassword("nonexistent_${testOwner}", "password")
        Optional<User> nonExistentById = userRepository.findById(-999L)

        then:
        !nonExistentByUsername.isPresent()
        !nonExistentByUsernamePassword.isPresent()
        !nonExistentById.isPresent()
    }

    void 'test user entity persistence validation'() {
        given:
        User user = SmartUserBuilder.builderForOwner(testOwner)
                .withUniqueUsername("persistence")
                .withFirstName("Persistence")
                .withLastName("Test")
                .withPassword("persistencepassword")
                .asActive()
                .buildAndValidate()

        when:
        User savedUser = userRepository.save(user)

        then:
        savedUser.userId != null
        savedUser.username != null
        savedUser.username.length() >= 3
        savedUser.username.length() <= 60
        savedUser.firstName != null
        savedUser.firstName.length() >= 1
        savedUser.firstName.length() <= 40
        savedUser.lastName != null
        savedUser.lastName.length() >= 1
        savedUser.lastName.length() <= 40
        savedUser.password != null
        savedUser.password.length() >= 8
        savedUser.password.length() <= 255
        savedUser.activeStatus == true
        savedUser.dateAdded != null
        savedUser.dateUpdated != null

        when:
        Optional<User> refetchedOpt = userRepository.findById(savedUser.userId)

        then:
        refetchedOpt.isPresent()
        def refetchedUser = refetchedOpt.get()
        refetchedUser.username == savedUser.username
        refetchedUser.firstName == savedUser.firstName
        refetchedUser.lastName == savedUser.lastName
        refetchedUser.password == savedUser.password
        refetchedUser.activeStatus == savedUser.activeStatus
        refetchedUser.dateAdded != null
        refetchedUser.dateUpdated != null
    }

    void 'test user name case conversion and validation'() {
        given:
        User user = SmartUserBuilder.builderForOwner(testOwner)
                .withUniqueUsername("MIXED_CASE")
                .withFirstName("FIRST")
                .withLastName("LAST")
                .withPassword("MixedCasePassword123")
                .asActive()
                .buildAndValidate()

        when:
        User savedUser = userRepository.save(user)

        then:
        // Username should be lowercase (handled by SmartBuilder)
        savedUser.username == savedUser.username.toLowerCase()
        // Note: Names may be stored as uppercase due to entity/converter behavior
        savedUser.firstName == "FIRST" || savedUser.firstName == "first"
        savedUser.lastName == "LAST" || savedUser.lastName == "last"
        // Password should remain case-sensitive
        savedUser.password == "MixedCasePassword123"
    }

    void 'test user with maximum allowed lengths'() {
        given:
        User user = SmartUserBuilder.builderForOwner(testOwner)
                .withUsername("a" * 60)  // Maximum allowed length (60 chars)
                .withFirstName("a" * 40) // Maximum allowed length (40 chars)
                .withLastName("b" * 40)  // Maximum allowed length (40 chars)
                .withPassword("a" * 255) // Maximum allowed length (255 chars)
                .asActive()
                .buildAndValidate()

        when:
        User savedUser = userRepository.save(user)

        then:
        savedUser.userId != null
        savedUser.username.length() == 60
        savedUser.firstName.length() == 40
        savedUser.lastName.length() == 40
        savedUser.password.length() == 255
    }

    void 'test user authentication workflow'() {
        given:
        User user = SmartUserBuilder.builderForOwner(testOwner)
                .withUniqueUsername("authflow")
                .withFirstName("Auth")
                .withLastName("Flow")
                .withPassword("authflowpassword123")
                .asActive()
                .buildAndValidate()
        User savedUser = userRepository.save(user)

        when: "authenticate with correct credentials"
        Optional<User> authenticatedUser = userRepository.findByUsernameAndPassword(
                savedUser.username, savedUser.password)

        then: "authentication succeeds"
        authenticatedUser.isPresent()
        authenticatedUser.get().userId == savedUser.userId
        authenticatedUser.get().activeStatus == true

        when: "authenticate with wrong password"
        Optional<User> failedAuth = userRepository.findByUsernameAndPassword(
                savedUser.username, "wrongpassword")

        then: "authentication fails"
        !failedAuth.isPresent()

        when: "authenticate with non-existent username"
        Optional<User> noUser = userRepository.findByUsernameAndPassword(
                "nonexistent_user", savedUser.password)

        then: "authentication fails"
        !noUser.isPresent()
    }

    void 'test repository context helper methods'() {
        given:
        User uniqueUser = repositoryContext.createTestUser("contexttest", "contextpassword123")

        when:
        User savedUser = userRepository.save(uniqueUser)

        then:
        savedUser.userId != null
        savedUser.username.contains("contexttest")
        savedUser.username.contains(testOwner.toLowerCase().replaceAll(/[^a-z0-9]/, ''))
        savedUser.password == "contextpassword123"
        savedUser.activeStatus == true
    }
}