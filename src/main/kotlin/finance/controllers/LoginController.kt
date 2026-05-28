package finance.controllers

import finance.domain.LoginRequest
import finance.domain.User
import finance.services.JwtTokenService
import finance.services.LoginAttemptService
import finance.services.TokenBlacklistService
import finance.services.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Date

@Tag(name = "Authentication", description = "Login, logout, registration, and user info")
@RestController
@RequestMapping("/api")
class LoginController(
    private val userService: UserService,
    private val tokenBlacklistService: TokenBlacklistService,
    private val loginAttemptService: LoginAttemptService,
    private val jwtTokenService: JwtTokenService,
) : BaseController() {
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

        val token = jwtTokenService.buildToken(loginRequest.username, loginRequest.keepLoggedIn)
        val cookie = jwtTokenService.buildTokenCookie(token, loginRequest.keepLoggedIn)
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
        val token = jwtTokenService.extractToken(request)

        if (!token.isNullOrBlank()) {
            try {
                val claims = jwtTokenService.parseClaims(token)
                val expirationTime = claims.expiration?.time ?: 0L
                val username = claims[JwtTokenService.CLAIM_USERNAME] as? String
                tokenBlacklistService.blacklistToken(token, expirationTime)
                logger.info("Token blacklisted for user: {}, expiration: {}", username, Date(expirationTime))
            } catch (e: Exception) {
                logger.warn("Failed to parse token during logout: {}", e.message)
            }
        }

        val cookie = jwtTokenService.buildClearCookie()
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
        if (bindingResult.hasErrors()) {
            val errors = bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid value") }
            logger.warn("Registration validation failed for username: {}", newUser.username.take(60))
            return ResponseEntity.badRequest().body(mapOf("errors" to errors.toString()))
        }

        if (!isValidRawPassword(newUser.password)) {
            logger.warn("Registration failed - invalid password format for username: ${newUser.username}")
            return ResponseEntity.badRequest().body(mapOf("error" to "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"))
        }

        logger.info("Register request received: {}", newUser.username.take(60))
        try {
            userService.signUp(newUser)
        } catch (e: IllegalArgumentException) {
            logger.warn("Registration failed - username already exists: ${newUser.username}")
            return ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to "Username already exists"))
        } catch (e: Exception) {
            logger.error("Registration error for username: {}", newUser.username.take(60))
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("error" to "Registration failed"))
        }

        logger.info("User registered, generating JWT")
        val token = jwtTokenService.buildToken(newUser.username)
        val cookie = jwtTokenService.buildTokenCookie(token, false)
        response.addHeader("Set-Cookie", cookie.toString())
        return ResponseEntity.status(HttpStatus.CREATED).body(mapOf("message" to "Registration successful"))
    }

    // curl -k --header "Cookie: token=your_jwt_token" --request POST https://localhost:8443/api/refresh
    @Operation(summary = "Refresh JWT cookie to extend an active session")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Token refreshed"),
            ApiResponse(responseCode = "401", description = "Not authenticated"),
        ],
    )
    @PostMapping("/refresh")
    fun refresh(
        @AuthenticationPrincipal principal: String?,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<Map<String, String>> {
        if (principal.isNullOrBlank()) {
            logger.warn("REFRESH_401 no authenticated principal")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Not authenticated"))
        }

        val keepLoggedIn =
            jwtTokenService.extractToken(request)?.let { currentToken ->
                try {
                    jwtTokenService.parseClaims(currentToken).get(JwtTokenService.CLAIM_KEEP_LOGGED_IN, Boolean::class.java) ?: false
                } catch (e: Exception) {
                    logger.warn("REFRESH could not read keepLoggedIn claim: {}", e.message)
                    false
                }
            } ?: false

        val token = jwtTokenService.buildToken(principal, keepLoggedIn)
        val cookie = jwtTokenService.buildTokenCookie(token, keepLoggedIn)
        response.addHeader("Set-Cookie", cookie.toString())
        logger.info("REFRESH_SUCCESS username={} keepLoggedIn={}", principal.take(60), keepLoggedIn)
        return ResponseEntity.ok(mapOf("message" to "Token refreshed"))
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

    private fun isValidRawPassword(password: String): Boolean {
        // SECURITY: Never accept pre-encoded passwords from user input
        if (password.startsWith("\$2a\$") || password.startsWith("\$2b\$") || password.startsWith("\$2y\$")) {
            logger.warn("SECURITY: Rejected pre-encoded password in registration attempt")
            return false
        }

        val passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$"
        return password.length >= 8 && password.matches(passwordRegex.toRegex())
    }
}
