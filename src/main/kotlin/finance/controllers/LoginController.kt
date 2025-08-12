package finance.controllers

import finance.domain.User
import finance.services.UserService
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import javax.crypto.SecretKey
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

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
        @RequestBody loginRequest: User,
        response: HttpServletResponse
    ): ResponseEntity<Void> {
        // Validate user credentials.
        val user = userService.signIn(loginRequest)
        logger.info("Login request received: ${loginRequest.username}")
        if (user.isEmpty) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        // Generate JWT after validating credentials.
        val now = Date()
        val expiration = Date(now.time + 60 * 60 * 1000) // 1 hour expiration
        val key: SecretKey = Keys.hmacShaKeyFor(jwtKey.toByteArray())
        val token = Jwts.builder()
            .claim("username", loginRequest.username)
            .notBefore(now)
            .expiration(expiration)
            .signWith(key)
            .compact()

        // Check if we're in a local development context (even if profile is prod)
        val isLocalDev = System.getenv("USERNAME")?.contains("henninb") == true || 
                        System.getenv("HOST_IP")?.contains("192.168") == true ||
                        activeProfile == "dev" || activeProfile == "development"
        
        val cookieBuilder = ResponseCookie.from("token", token)
            .path("/")
            .maxAge(7 * 24 * 60 * 60)
            .httpOnly(false) // Allow JavaScript access in development for debugging
            .secure(false) // Never require HTTPS for local development
            .sameSite("Lax") // Use Lax for local development
        
        // Configure domain based on environment
        val cookie = if (isLocalDev) {
            cookieBuilder.build() // No domain restriction for local development
        } else {
            cookieBuilder.domain(".bhenning.com").build() // Domain for production
        }

        response.addHeader("Set-Cookie", cookie.toString())
        return ResponseEntity.noContent().build()
    }

    // curl -k --header "Content-Type: application/json" --request POST https://localhost:8443/api/logout
    @PostMapping("/logout")
    fun logout(response: HttpServletResponse): ResponseEntity<Void> {
        // Check if we're in a local development context (even if profile is prod)
        val isLocalDev = System.getenv("USERNAME")?.contains("henninb") == true || 
                        System.getenv("HOST_IP")?.contains("192.168") == true ||
                        activeProfile == "dev" || activeProfile == "development"
        
        val cookieBuilder = ResponseCookie.from("token", "")
            .path("/")
            .maxAge(0)
            .httpOnly(false) // Allow JavaScript access in development for debugging
            .secure(false) // Never require HTTPS for local development  
            .sameSite("Lax") // Use Lax for local development
        
        // Configure domain based on environment
        val cookie = if (isLocalDev) {
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
        @RequestBody newUser: User,
        response: HttpServletResponse
    ): ResponseEntity<Void> {
        logger.info("Register request received: $newUser")
        try {
            // Register the new user.
            userService.signUp(newUser)
        } catch (e: IllegalArgumentException) {
            logger.info("Username ${newUser.username} already exists.")
            return ResponseEntity.status(HttpStatus.CONFLICT).build()
        }

        // Auto-login: generate a JWT token for the new user.
        logger.info("User registered, generating JWT")
        val now = Date()
        val expiration = Date(now.time + 60 * 60 * 1000) // 1 hour expiration

        val key: SecretKey = Keys.hmacShaKeyFor(jwtKey.toByteArray())
        val token = Jwts.builder()
            .claim("username", newUser.username)
            .notBefore(now)
            .expiration(expiration)
            .signWith(key)
            .compact()

        // Check if we're in a local development context (even if profile is prod)
        val isLocalDev = System.getenv("USERNAME")?.contains("henninb") == true || 
                        System.getenv("HOST_IP")?.contains("192.168") == true ||
                        activeProfile == "dev" || activeProfile == "development"
        
        val cookieBuilder = ResponseCookie.from("token", token)
            .httpOnly(false) // Allow JavaScript access in development for debugging
            .secure(false) // Never require HTTPS for local development
            .maxAge(24 * 60 * 60)
            .sameSite("Lax") // Use Lax for local development
            .path("/")
        
        // Configure domain based on environment
        val cookie = if (isLocalDev) {
            cookieBuilder.build() // No domain restriction for local development
        } else {
            cookieBuilder.domain(".bhenning.com").build() // Domain for production
        }

        response.addHeader("Set-Cookie", cookie.toString())
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    // curl -k --header "Cookie: token=your_jwt_token" https://localhost:8443/api/me
    @GetMapping("/me")
    fun getCurrentUser(@CookieValue(name = "token", required = false) token: String?): ResponseEntity<Any> {
        // Check if the token cookie is present.
        if (token.isNullOrBlank()) {
            logger.info("No token found in the request")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        try {
            // Parse and validate the JWT.
            val key: SecretKey = Keys.hmacShaKeyFor(jwtKey.toByteArray())
            val claims = Jwts.parser()
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
            logger.error("JWT validation failed: ${e.message}")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }
}
