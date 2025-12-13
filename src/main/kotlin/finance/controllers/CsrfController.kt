package finance.controllers

import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller for CSRF token management.
 * Provides endpoint for Single Page Applications and REST clients to fetch CSRF tokens.
 *
 * Security Notes:
 * - This endpoint is public (no authentication required) to allow token fetch before login
 * - The token itself is not sensitive; it's only valid when combined with the HTTP-only session cookie
 * - Token is automatically set as XSRF-TOKEN cookie by Spring Security
 * - Clients should read the cookie and include it in X-CSRF-TOKEN header for mutations
 */
@CrossOrigin
@RestController
@RequestMapping("/api")
class CsrfController : BaseController() {
    /**
     * Endpoint to retrieve CSRF token for client-side requests.
     *
     * The CsrfToken parameter is automatically injected by Spring Security.
     * When this endpoint is called, Spring Security:
     * 1. Generates a CSRF token (if not already present)
     * 2. Sets XSRF-TOKEN cookie in the response
     * 3. Provides token details in the response body
     *
     * @param token CSRF token injected by Spring Security
     * @return Map containing token value, header name, and parameter name
     *
     * Example Response:
     * {
     *   "token": "abc123...",
     *   "headerName": "X-CSRF-TOKEN",
     *   "parameterName": "_csrf"
     * }
     */
    @GetMapping("/csrf")
    fun getCsrfToken(token: CsrfToken): Map<String, String> {
        logger.info("CSRF token requested")
        return mapOf(
            "token" to token.token,
            "headerName" to token.headerName,
            "parameterName" to token.parameterName,
        )
    }
}
