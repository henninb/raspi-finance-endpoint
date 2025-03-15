package finance.controllers

import finance.domain.User
import finance.services.UserService
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@CrossOrigin
@RestController
@RequestMapping("/api")
class LoginController(private val userService: UserService) : BaseController() {

    // TODO: inject this from a secure config on the database
    // fetch it once from the configuration
    private val JWT_KEY = "your_jwt_key"

    @PostMapping("/login")
    fun login(
        @RequestBody loginRequest: User,
        response: HttpServletResponse
    ): ResponseEntity<Void> {
        // Validate user credentials.
        val user = userService.signIn(loginRequest.username, loginRequest.password)
        logger.info("user: $user")
        if (user.isEmpty) {
            logger.info("Invalid login attempt")
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        logger.info("Generating JWT")
        val now = Date()
        val expiration = Date(now.time + 60 * 60 * 1000) // 1 hour expiration

        val token = Jwts.builder()
            .claim("username", loginRequest.username)
            .setNotBefore(now)
            .setExpiration(expiration)
            .signWith(SignatureAlgorithm.HS256, JWT_KEY.toByteArray())
            .compact()

        // Set the token in an HTTP-only, secure cookie.
        val cookie = Cookie("token", token).apply {
            isHttpOnly = true
            secure = true // Set to true for production environments using HTTPS
            maxAge = 24 * 60 * 60  // 24 hours
            path = "/"
        }
        response.addCookie(cookie)

        // Return 204 No Content with no response body.
        return ResponseEntity.noContent().build()
    }
}