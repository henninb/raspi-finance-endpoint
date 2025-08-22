package finance.helpers

import finance.domain.User
import groovy.util.logging.Slf4j
import java.util.concurrent.atomic.AtomicInteger

@Slf4j
class SmartUserBuilder {

    private static final AtomicInteger COUNTER = new AtomicInteger(0)

    private String testOwner
    private String username
    private String password = 'test_password'
    private Boolean activeStatus = true
    private String firstName = 'test'
    private String lastName = 'user'

    private SmartUserBuilder(String testOwner) {
        this.testOwner = testOwner
        this.username = generateUniqueUsername()
    }

    static SmartUserBuilder builderForOwner(String testOwner) {
        return new SmartUserBuilder(testOwner)
    }

    private String generateUniqueUsername() {
        String counter = COUNTER.incrementAndGet().toString()
        String ownerPart = testOwner?.toString()?.toLowerCase()?.replaceAll(/[^a-z0-9]/, '') ?: 'test'
        if (ownerPart.isEmpty()) ownerPart = 'test'

        String base = "user${counter}_${ownerPart}"

        // Ensure reasonable length constraints (3-50)
        if (base.length() > 50) {
            String shortOwner = ownerPart.length() > 12 ? ownerPart[0..11] : ownerPart
            base = "u${counter}_${shortOwner}"
        }
        if (base.length() < 3) base = 'usr'

        String cleaned = base.toLowerCase()
        log.debug("Generated username: ${cleaned} for test owner: ${testOwner}")
        return cleaned
    }

    User build() {
        User user = new User().with {
            userId = 0L
            username = this.username
            password = this.password
            activeStatus = this.activeStatus
            firstName = this.firstName
            lastName = this.lastName
            return it
        }
        return user
    }

    User buildAndValidate() {
        User user = build()
        validateConstraints(user)
        return user
    }

    private void validateConstraints(User user) {
        // Username: 3-50, lowercase letters/numbers/underscore/dash
        if (user.username == null || user.username.length() < 3 || user.username.length() > 50) {
            throw new IllegalStateException("Username '${user.username}' violates length constraints (3-50 chars)")
        }
        if (!user.username.matches(/^[a-z0-9_-]*$/)) {
            throw new IllegalStateException("Username '${user.username}' violates allowed pattern (letters/numbers/_/- only)")
        }

        // First/last name: allow 1-50 characters (loose check for tests)
        if (user.firstName == null || user.firstName.length() < 1 || user.firstName.length() > 50) {
            throw new IllegalStateException("First name '${user.firstName}' violates length constraints (1-50 chars)")
        }
        if (user.lastName == null || user.lastName.length() < 1 || user.lastName.length() > 50) {
            throw new IllegalStateException("Last name '${user.lastName}' violates length constraints (1-50 chars)")
        }

        log.debug("User passed constraint validation: ${user.username}")
    }

    // Fluent API
    SmartUserBuilder withUsername(String username) {
        this.username = username?.toLowerCase()
        return this
    }

    SmartUserBuilder withUniqueUsername(String prefix = 'user') {
        this.username = generateUniqueUsername(prefix)
        return this
    }

    private String generateUniqueUsername(String prefix) {
        String cleanPrefix = prefix?.toLowerCase()?.replaceAll(/[^a-z0-9]/, '') ?: 'user'
        if (cleanPrefix.isEmpty()) cleanPrefix = 'user'

        String ownerPart = testOwner?.toLowerCase()?.replaceAll(/[^a-z0-9]/, '') ?: 'test'
        if (ownerPart.isEmpty()) ownerPart = 'test'

        String base = "${cleanPrefix}_${ownerPart}"
        if (base.length() > 50) {
            String shortOwner = ownerPart.length() > 12 ? ownerPart[0..11] : ownerPart
            base = "${cleanPrefix}_${shortOwner}"
        }
        if (base.length() < 3) base = 'usr'
        return base.toLowerCase()
    }

    SmartUserBuilder withPassword(String password) {
        this.password = password
        return this
    }

    SmartUserBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        return this
    }

    SmartUserBuilder withFirstName(String firstName) {
        this.firstName = firstName
        return this
    }

    SmartUserBuilder withLastName(String lastName) {
        this.lastName = lastName
        return this
    }

    // Convenience
    SmartUserBuilder asActive() {
        this.activeStatus = true
        return this
    }

    SmartUserBuilder asInactive() {
        this.activeStatus = false
        return this
    }
}

