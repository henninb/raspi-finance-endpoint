package finance.controllers

import finance.domain.LoginResponse
import finance.domain.User
import finance.services.UserService
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*


@CrossOrigin
@RestController
@RequestMapping("/api")
class LoginController(private val userService: UserService): BaseController() {

    private val JWT_KEY = "your_jwt_key" // Ideally, inject this from a secure config

    @PostMapping("/login")
    fun login(
        @RequestBody loginRequest: User,
        response: HttpServletResponse
    ): ResponseEntity<LoginResponse> {
        // Query your database for a matching user using a UserService.
        val user = userService.signIn(loginRequest.username, loginRequest.password)
        logger.info("user:$user")
        if (user.isEmpty) {
            logger.info("is empty")
            // If no matching user is found, return a 403 Forbidden response.
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(LoginResponse(error = "Failed login attempt."))
        }

        logger.info("gnerate JWT")
        // Generate JWT token if user exists.
        val now = Date()
        val expiration = Date(now.time + 60 * 60 * 1000) // 1 hour expiration

        val token = Jwts.builder()
            .claim("username", loginRequest.username)
            .setNotBefore(now)
            .setExpiration(expiration)
            .signWith(SignatureAlgorithm.HS256, JWT_KEY.toByteArray())
            .compact()

        // Optionally, set the token in an HTTP-only cookie.
        val cookie = Cookie("token", token).apply {
            isHttpOnly = true
            secure = false // Set to true in production (HTTPS)
            maxAge = 24 * 60 * 60  // 24 hours
            path = "/"
        }
        response.addCookie(cookie)

        // Return the token in the JSON response.
        return ResponseEntity.ok(LoginResponse(token = token))
    }
}