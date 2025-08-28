package finance.helpers

import finance.domain.LoginRequest
import groovy.util.logging.Slf4j
import java.util.concurrent.atomic.AtomicInteger

@Slf4j
class SmartLoginRequestBuilder {

    private static final AtomicInteger COUNTER = new AtomicInteger(0)

    private String testOwner
    private String username
    private String password = 'test_password'

    private SmartLoginRequestBuilder(String testOwner) {
        this.testOwner = testOwner
        this.username = generateUniqueUsername()
    }

    static SmartLoginRequestBuilder builderForOwner(String testOwner) {
        return new SmartLoginRequestBuilder(testOwner)
    }

    private String generateUniqueUsername() {
        String counter = COUNTER.incrementAndGet().toString()
        String ownerPart = testOwner?.toString()?.toLowerCase()?.replaceAll(/[^a-z0-9]/, '') ?: 'test'
        if (ownerPart.isEmpty()) ownerPart = 'test'

        String base = "user${counter}_${ownerPart}"

        // Ensure reasonable length constraints for username
        if (base.length() > 50) {
            String shortOwner = ownerPart.length() > 12 ? ownerPart[0..11] : ownerPart
            base = "u${counter}_${shortOwner}"
        }
        if (base.length() < 3) base = 'usr'

        String cleaned = base.toLowerCase()
        log.debug("Generated login username: ${cleaned} for test owner: ${testOwner}")
        return cleaned
    }

    LoginRequest build() {
        LoginRequest loginRequest = new LoginRequest(
            username: this.username,
            password: this.password
        )
        return loginRequest
    }

    LoginRequest buildAndValidate() {
        LoginRequest loginRequest = build()
        validateConstraints(loginRequest)
        return loginRequest
    }

    private void validateConstraints(LoginRequest loginRequest) {
        // Username: NotBlank constraint
        if (loginRequest.username == null || loginRequest.username.trim().isEmpty()) {
            throw new IllegalStateException("Username cannot be blank - violates @NotBlank constraint")
        }

        // Password: NotBlank constraint
        if (loginRequest.password == null || loginRequest.password.trim().isEmpty()) {
            throw new IllegalStateException("Password cannot be blank - violates @NotBlank constraint")
        }

        log.debug("LoginRequest passed constraint validation: ${loginRequest.username}")
    }

    // Fluent API
    SmartLoginRequestBuilder withUsername(String username) {
        this.username = username
        return this
    }

    SmartLoginRequestBuilder withUniqueUsername(String prefix = 'user') {
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

    SmartLoginRequestBuilder withPassword(String password) {
        this.password = password
        return this
    }

    // Convenience methods for common test scenarios
    SmartLoginRequestBuilder withValidCredentials() {
        this.password = 'valid_password123'
        return this
    }

    SmartLoginRequestBuilder withInvalidPassword() {
        this.password = 'wrong_pass'
        return this
    }

    SmartLoginRequestBuilder withEmptyUsername() {
        this.username = ''
        return this
    }

    SmartLoginRequestBuilder withEmptyPassword() {
        this.password = ''
        return this
    }
}