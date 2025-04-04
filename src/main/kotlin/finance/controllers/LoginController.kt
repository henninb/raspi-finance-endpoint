package finance.controllers

import finance.domain.User
import finance.services.UserService
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
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
        val token = Jwts.builder()
            .claim("username", loginRequest.username)
            .setNotBefore(now)
            .setExpiration(expiration)
            .signWith(SignatureAlgorithm.HS256, jwtKey.toByteArray())
            .compact()

        val cookie = ResponseCookie.from("token", token)
            .domain(".bhenning.com")
            .path("/")
            .maxAge(7 * 24 * 60 * 60)
            .httpOnly(true)
            .secure(true)
            .sameSite("None")
            .build()

        response.addHeader("Set-Cookie", cookie.toString())
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/logout")
    fun logout(response: HttpServletResponse): ResponseEntity<Void> {
        val cookie = ResponseCookie.from("token", "")
            .domain(".bhenning.com")
            .path("/")
            .maxAge(0)
            .httpOnly(true)
            .secure(true)
            .sameSite("None")
            .build()

        response.addHeader("Set-Cookie", cookie.toString())
        return ResponseEntity.noContent().build()
    }

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

        val token = Jwts.builder()
            .claim("username", newUser.username)
            .setNotBefore(now)
            .setExpiration(expiration)
            .signWith(SignatureAlgorithm.HS256, jwtKey.toByteArray())
            .compact()

        val cookie = ResponseCookie.from("token", token)
            .httpOnly(true)
            .secure(true)
            .maxAge(24 * 60 * 60)
            .sameSite("None") // needed for cross-origin cookie sharing
            .path("/")
            .build()

        response.addHeader("Set-Cookie", cookie.toString())
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @GetMapping("/me")
    fun getCurrentUser(@CookieValue(name = "token", required = false) token: String?): ResponseEntity<Any> {
        // Check if the token cookie is present.
        if (token.isNullOrBlank()) {
            logger.info("No token found in the request")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        try {
            // Parse and validate the JWT.
            val claims = Jwts.parser()
                .setSigningKey(jwtKey.toByteArray())
                .parseClaimsJws(token)
                .body

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
