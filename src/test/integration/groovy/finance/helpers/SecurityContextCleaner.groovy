package finance.helpers

import org.springframework.security.core.context.SecurityContextHolder

/**
 * AutoCloseable cleaner to clear Spring SecurityContext after each test.
 */
class SecurityContextCleaner implements AutoCloseable {
    @Override
    void close() {
        SecurityContextHolder.clearContext()
    }
}

