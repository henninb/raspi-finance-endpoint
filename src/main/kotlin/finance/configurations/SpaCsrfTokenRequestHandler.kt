package finance.configurations

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.security.web.csrf.CsrfTokenRequestHandler
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler
import java.util.function.Supplier

/**
 * CSRF token handler for Single Page Applications and REST APIs.
 * Implements deferred token loading pattern where token is only generated
 * when explicitly requested via /api/csrf endpoint.
 *
 * This handler uses the XOR encoding pattern to protect against BREACH attacks
 * while allowing JavaScript clients to access the token for inclusion in request headers.
 *
 * Token Flow:
 * 1. Client requests token from /api/csrf endpoint
 * 2. Spring Security generates token and stores in XSRF-TOKEN cookie
 * 3. Client reads cookie value and includes in X-CSRF-TOKEN header for mutations
 * 4. This handler validates the token on protected endpoints
 */
class SpaCsrfTokenRequestHandler : CsrfTokenRequestHandler {
    private val delegate = XorCsrfTokenRequestAttributeHandler()

    /**
     * Handle CSRF token for the request.
     * Delegates to XorCsrfTokenRequestAttributeHandler for encoding/decoding.
     */
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        deferredCsrfToken: Supplier<CsrfToken>,
    ) {
        delegate.handle(request, response, deferredCsrfToken)
    }

    /**
     * Resolve CSRF token value from request.
     * Delegates to XorCsrfTokenRequestAttributeHandler which:
     * 1. Checks the header specified in csrfToken.headerName (X-CSRF-TOKEN)
     * 2. Decodes the XOR-encoded token for BREACH attack protection
     * 3. Returns the decoded value for validation against the cookie
     */
    override fun resolveCsrfTokenValue(
        request: HttpServletRequest,
        csrfToken: CsrfToken,
    ): String? {
        // Delegate handles both header extraction AND XOR decoding
        return delegate.resolveCsrfTokenValue(request, csrfToken)
    }
}
