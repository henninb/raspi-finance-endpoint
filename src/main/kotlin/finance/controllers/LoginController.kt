package finance.controllers

import finance.domain.LoginRequest
import finance.domain.User
import finance.services.UserService
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Date
import javax.crypto.SecretKey

@CrossOrigin
@RestController
@RequestMapping("/api")
class LoginController(private val userService: UserService) : BaseController() {
    @Value("\${custom.project.jwt.key}")
    private lateinit var jwtKey: String

    @Value("\${spring.profiles.active:dev}")
    private lateinit var activeProfile: String

    // curl -k --header "Content-Type: application/json" --request POST --data '{"username": "testuser", "password": "password123"}' https://localhost:8443/api/login
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody loginRequest: LoginRequest,
        bindingResult: BindingResult,
        response: HttpServletResponse,
    ): ResponseEntity<Map<String, String>> {
        // Entry log for tracing
        logger.info("LOGIN_REQUEST username=${loginRequest.username}")

        // Check for validation errors on the minimal DTO
        if (bindingResult.hasErrors()) {
            val errors = bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid value") }
            logger.warn("LOGIN_400_VALIDATION username=${loginRequest.username} errors=$errors")
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
                logger.warn("LOGIN_401_EXCEPTION username=${loginRequest.username} ex=${e.javaClass.simpleName} msg=${e.message}")
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Invalid credentials"))
            }

        logger.info("LOGIN_AUTH_ATTEMPT username=${loginRequest.username}")
        if (user.isEmpty) {
            logger.warn("LOGIN_401_INVALID_CREDENTIALS username=${loginRequest.username}")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Invalid credentials"))
        }

        // Generate JWT after validating credentials.
        val now = Date()
        val expiration = Date(now.time + 60 * 60 * 1000) // 1 hour expiration
        val key: SecretKey = Keys.hmacShaKeyFor(jwtKey.toByteArray())
        val token =
            Jwts.builder()
                .claim("username", loginRequest.username)
                .notBefore(now)
                .expiration(expiration)
                .signWith(key)
                .compact()

        // Check if we're in a local development context (even if profile is prod)
        val isLocalDev =
            System.getenv("USERNAME")?.contains("henninb") == true ||
                System.getenv("HOST_IP")?.contains("192.168") == true ||
                activeProfile == "dev" || activeProfile == "development"

        val cookieBuilder =
            ResponseCookie.from("token", token)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .httpOnly(true) // Prevent XSS token theft
                .secure(!isLocalDev) // Require HTTPS in production
                .sameSite(if (isLocalDev) "Lax" else "Strict") // Strict CSRF protection in production

        // Configure domain based on environment
        val cookie =
            if (isLocalDev) {
                cookieBuilder.build() // No domain restriction for local development
            } else {
                cookieBuilder.domain(".bhenning.com").build() // Domain for production
            }

        response.addHeader("Set-Cookie", cookie.toString())
        logger.info("LOGIN_SUCCESS username=${loginRequest.username}")
        return ResponseEntity.ok(mapOf("message" to "Login successful"))
    }

    // curl -k --header "Content-Type: application/json" --request POST https://localhost:8443/api/logout
    @PostMapping("/logout")
    fun logout(response: HttpServletResponse): ResponseEntity<Void> {
        // Check if we're in a local development context (even if profile is prod)
        val isLocalDev =
            System.getenv("USERNAME")?.contains("henninb") == true ||
                System.getenv("HOST_IP")?.contains("192.168") == true ||
                activeProfile == "dev" || activeProfile == "development"

        val cookieBuilder =
            ResponseCookie.from("token", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true) // Prevent XSS token theft
                .secure(!isLocalDev) // Require HTTPS in production
                .sameSite(if (isLocalDev) "Lax" else "Strict") // Strict CSRF protection in production

        // Configure domain based on environment
        val cookie =
            if (isLocalDev) {
                cookieBuilder.build() // No domain restriction for local development
            } else {
                cookieBuilder.domain(".bhenning.com").build() // Domain for production
            }

        response.addHeader("Set-Cookie", cookie.toString())
        return ResponseEntity.noContent().build()
    }

    // curl -k --header "Content-Type: application/json" --request POST --data '{"username": "newuser", "password": "password123", "email": "user@example.com"}' https://localhost:8443/api/register
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
        val expiration = Date(now.time + 60 * 60 * 1000) // 1 hour expiration

        val key: SecretKey = Keys.hmacShaKeyFor(jwtKey.toByteArray())
        val token =
            Jwts.builder()
                .claim("username", newUser.username)
                .notBefore(now)
                .expiration(expiration)
                .signWith(key)
                .compact()

        // Check if we're in a local development context (even if profile is prod)
        val isLocalDev =
            System.getenv("USERNAME")?.contains("henninb") == true ||
                System.getenv("HOST_IP")?.contains("192.168") == true ||
                activeProfile == "dev" || activeProfile == "development"

        val cookieBuilder =
            ResponseCookie.from("token", token)
                .httpOnly(true) // Prevent XSS token theft
                .secure(!isLocalDev) // Require HTTPS in production
                .maxAge(24 * 60 * 60)
                .sameSite(if (isLocalDev) "Lax" else "Strict") // Strict CSRF protection in production
                .path("/")

        // Configure domain based on environment
        val cookie =
            if (isLocalDev) {
                cookieBuilder.build() // No domain restriction for local development
            } else {
                cookieBuilder.domain(".bhenning.com").build() // Domain for production
            }

        response.addHeader("Set-Cookie", cookie.toString())
        return ResponseEntity.status(HttpStatus.CREATED).body(mapOf("message" to "Registration successful"))
    }

    // curl -k --header "Cookie: token=your_jwt_token" https://localhost:8443/api/me
    @GetMapping("/me")
    fun getCurrentUser(
        @CookieValue(name = "token", required = false) token: String?,
    ): ResponseEntity<Any> {
        // Check if the token cookie is present.
        if (token.isNullOrBlank()) {
            logger.info("No token found in the request")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        try {
            // Parse and validate the JWT.
            val key: SecretKey = Keys.hmacShaKeyFor(jwtKey.toByteArray())
            val claims =
                Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .payload

            val username = claims["username"] as? String
            if (username.isNullOrBlank()) {
                logger.info("Token does not contain a valid username claim")
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
            }

            // Optionally, fetch the full user details from your database.
            val user = userService.findUserByUsername(username)
            if (user == null) {
                logger.info("No user found for username: $username")
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
            }

            // Return user information (excluding sensitive data).
            return ResponseEntity.ok(user)
        } catch (e: Exception) {
            logger.warn("JWT validation failed for token validation")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Invalid or expired token"))
        }
    }

    /**
     * Validates raw password format before encoding
     */
    private fun isValidRawPassword(password: String): Boolean {
        // Skip validation for already encoded passwords (BCrypt hashes start with $2a$ or $2b$)
        if (password.startsWith("$2a$") || password.startsWith("$2b$")) {
            return true
        }

        val passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$"
        return password.length >= 8 && password.matches(passwordRegex.toRegex())
    }
}
