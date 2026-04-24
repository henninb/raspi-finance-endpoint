package finance.controllers

import finance.domain.LoginRequest
import finance.domain.User
import finance.services.LoginAttemptService
import finance.services.TokenBlacklistService
import finance.services.UserService
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Date
import javax.crypto.SecretKey

@Tag(name = "Authentication", description = "Login, logout, registration, and user info")
@RestController
@RequestMapping("/api")
class LoginController(
    private val userService: UserService,
    private val tokenBlacklistService: TokenBlacklistService,
    private val loginAttemptService: LoginAttemptService,
    @Value("\${custom.project.jwt.key}") jwtKey: String,
) : BaseController() {
    private val secretKey: SecretKey = Keys.hmacShaKeyFor(jwtKey.toByteArray(Charsets.UTF_8))

    @Value("\${spring.profiles.active:dev}")
    private lateinit var activeProfile: String

    companion object {
        private const val JWT_EXPIRY_MS = 60 * 60 * 1000L // 1 hour
        private const val JWT_EXPIRY_SECONDS = 3600L
    }

    private val isSecureCookie: Boolean
        get() = activeProfile != "dev" && activeProfile != "development"

    // curl -k --header "Content-Type: application/json" --request POST --data '{"username": "testuser", "password": "password123"}' https://localhost:8443/api/login
    @Operation(summary = "Authenticate and issue JWT cookie")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Login successful"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "401", description = "Invalid credentials"),
        ],
    )
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody loginRequest: LoginRequest,
        bindingResult: BindingResult,
        response: HttpServletResponse,
    ): ResponseEntity<Map<String, String>> {
        logger.info("LOGIN_REQUEST username={}", loginRequest.username.take(60).replace(Regex("[\\r\\n\\t]"), "_"))

        if (loginAttemptService.isLocked(loginRequest.username)) {
            val remaining = loginAttemptService.remainingLockSeconds(loginRequest.username)
            logger.warn("LOGIN_429_LOCKED username={} remainingSeconds={}", loginRequest.username.take(60), remaining)
            return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(mapOf("error" to "Account temporarily locked. Try again in $remaining seconds."))
        }

        if (bindingResult.hasErrors()) {
            val errors = bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid value") }
            logger.warn("LOGIN_400_VALIDATION username={} errors={}", loginRequest.username.take(60), errors)
            return ResponseEntity.badRequest().body(mapOf("errors" to errors.toString()))
        }

        // Validate user credentials against stored hash (no strength checks here)
        val authAttempt =
            User().apply {
                username = loginRequest.username
                password = loginRequest.password
            }

        val user =
            try {
                userService.signIn(authAttempt)
            } catch (e: Exception) {
                logger.warn("LOGIN_401_EXCEPTION username={} ex={} msg={}", loginRequest.username.take(60), e.javaClass.simpleName, e.message)
                loginAttemptService.recordFailure(loginRequest.username)
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Invalid credentials"))
            }

        logger.info("LOGIN_AUTH_ATTEMPT username={}", loginRequest.username.take(60))
        if (user.isEmpty) {
            logger.warn("LOGIN_401_INVALID_CREDENTIALS username={}", loginRequest.username.take(60))
            loginAttemptService.recordFailure(loginRequest.username)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Invalid credentials"))
        }
        loginAttemptService.recordSuccess(loginRequest.username)

        // Generate JWT after validating credentials.
        val now = Date()
        val expiration = Date(now.time + JWT_EXPIRY_MS)
        val token =
            Jwts
                .builder()
                .issuer("raspi-finance-endpoint")
                .audience()
                .add("raspi-finance-endpoint")
                .and()
                .subject(loginRequest.username)
                .claim("username", loginRequest.username)
                .issuedAt(now)
                .notBefore(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact()

        val cookie =
            ResponseCookie
                .from("token", token)
                .path("/")
                .maxAge(JWT_EXPIRY_SECONDS)
                .httpOnly(true)
                .secure(isSecureCookie)
                .sameSite(if (isSecureCookie) "Strict" else "Lax")
                .build()

        response.addHeader("Set-Cookie", cookie.toString())
        logger.info("LOGIN_SUCCESS username={}", loginRequest.username.take(60))
        return ResponseEntity.ok(mapOf("message" to "Login successful"))
    }

    // curl -k --header "Content-Type: application/json" --request POST https://localhost:8443/api/logout
    @Operation(summary = "Invalidate JWT cookie and logout")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Logout successful"),
        ],
    )
    @PostMapping("/logout")
    fun logout(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<Void> {
        // Extract token from cookie or Authorization header
        var token: String? =
            run {
                request.cookies?.firstOrNull { it.name == "token" }?.value
                    ?: request
                        .getHeader("Cookie")
                        ?.split(';')
                        ?.map { it.trim() }
                        ?.firstOrNull { it.startsWith("token=") }
                        ?.substringAfter("token=")
            }

        if (token.isNullOrBlank()) {
            val authHeader = request.getHeader("Authorization")
            if (!authHeader.isNullOrBlank() && authHeader.startsWith("Bearer ")) {
                token = authHeader.removePrefix("Bearer ").trim()
            }
        }

        // If token exists, blacklist it before clearing the cookie
        if (!token.isNullOrBlank()) {
            try {
                val claims =
                    Jwts
                        .parser()
                        .requireIssuer("raspi-finance-endpoint")
                        .requireAudience("raspi-finance-endpoint")
                        .verifyWith(secretKey)
                        .build()
                        .parseSignedClaims(token)
                        .payload

                val expirationTime = claims.expiration?.time ?: 0L
                val username = claims["username"] as? String

                // Blacklist the token with its expiration time
                tokenBlacklistService.blacklistToken(token, expirationTime)
                logger.info("Token blacklisted for user: $username, expiration: ${Date(expirationTime)}")
            } catch (e: Exception) {
                // Token might be invalid or expired, but we still clear the cookie
                logger.warn("Failed to parse token during logout: ${e.message}")
            }
        }

        val cookie =
            ResponseCookie
                .from("token", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(isSecureCookie)
                .sameSite(if (isSecureCookie) "Strict" else "Lax")
                .build()

        response.addHeader("Set-Cookie", cookie.toString())
        logger.info("Logout successful, cookie cleared")
        return ResponseEntity.noContent().build()
    }

    // curl -k --header "Content-Type: application/json" --request POST --data '{"username": "newuser", "password": "password123", "email": "user@example.com"}' https://localhost:8443/api/register
    @Operation(summary = "Register new user and issue JWT cookie")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Registration successful"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "409", description = "Username already exists"),
            ApiResponse(responseCode = "500", description = "Server error during registration"),
        ],
    )
    @PostMapping("/register", consumes = ["application/json"])
    fun register(
        @Valid @RequestBody newUser: User,
        bindingResult: BindingResult,
        response: HttpServletResponse,
    ): ResponseEntity<Map<String, String>> {
        // Check for validation errors
        if (bindingResult.hasErrors()) {
            val errors = bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid value") }
            logger.warn("Registration validation failed for username: ${newUser.username}")
            return ResponseEntity.badRequest().body(mapOf("errors" to errors.toString()))
        }

        // Additional password validation for raw passwords (before encoding)
        if (!isValidRawPassword(newUser.password)) {
            logger.warn("Registration failed - invalid password format for username: ${newUser.username}")
            return ResponseEntity.badRequest().body(mapOf("error" to "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"))
        }

        logger.info("Register request received: ${newUser.username}")
        try {
            // Register the new user
            userService.signUp(newUser)
        } catch (e: IllegalArgumentException) {
            logger.warn("Registration failed - username already exists: ${newUser.username}")
            return ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to "Username already exists"))
        } catch (e: Exception) {
            logger.error("Registration error for username: ${newUser.username}")
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("error" to "Registration failed"))
        }

        // Auto-login: generate a JWT token for the new user.
        logger.info("User registered, generating JWT")
        val now = Date()
        val expiration = Date(now.time + JWT_EXPIRY_MS)

        val token =
            Jwts
                .builder()
                .issuer("raspi-finance-endpoint")
                .audience()
                .add("raspi-finance-endpoint")
                .and()
                .subject(newUser.username)
                .claim("username", newUser.username)
                .issuedAt(now)
                .notBefore(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact()

        val cookie =
            ResponseCookie
                .from("token", token)
                .httpOnly(true)
                .secure(isSecureCookie)
                .maxAge(JWT_EXPIRY_SECONDS)
                .sameSite(if (isSecureCookie) "Strict" else "Lax")
                .path("/")
                .build()

        response.addHeader("Set-Cookie", cookie.toString())
        return ResponseEntity.status(HttpStatus.CREATED).body(mapOf("message" to "Registration successful"))
    }

    // curl -k --header "Cookie: token=your_jwt_token" https://localhost:8443/api/me
    @Operation(summary = "Return current user info from authenticated session")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "User info returned"),
            ApiResponse(responseCode = "401", description = "Not authenticated"),
            ApiResponse(responseCode = "404", description = "User not found"),
        ],
    )
    @GetMapping("/me")
    fun getCurrentUser(
        @AuthenticationPrincipal principal: String?,
    ): ResponseEntity<Any> {
        if (principal.isNullOrBlank()) {
            logger.info("No authenticated principal found")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        val user = userService.findUserByUsername(principal)
        if (user == null) {
            logger.info("No user found for authenticated principal: {}", principal)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }

        return ResponseEntity.ok(user)
    }

    /**
     * Validates raw password format before encoding
     */
    private fun isValidRawPassword(password: String): Boolean {
        // SECURITY: Never accept pre-encoded passwords from user input
        // Pre-encoded passwords would bypass complexity requirements
        if (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$")) {
            logger.warn("SECURITY: Rejected pre-encoded password in registration attempt")
            return false
        }

        val passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$"
        return password.length >= 8 && password.matches(passwordRegex.toRegex())
    }
}
