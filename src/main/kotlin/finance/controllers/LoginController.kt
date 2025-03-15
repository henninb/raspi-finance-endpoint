package finance.controllers

import finance.domain.User
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*


@CrossOrigin
@RestController
@RequestMapping("/api")
class LoginController {

//    private val EMAIL = "henninb@gmail.com"      // Replace with your email
//    private val PASSWORD = "monday1"             // Replace with your password
    private val JWT_KEY = "your_jwt_key"           // Replace with your JWT key (ideally keep it in config)

    @PostMapping("/login", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun login(@RequestBody loginRequest: User, response: HttpServletResponse): ResponseEntity<Map<String, String>> {
        try {
            // Current time
            val now = Date()
            // Set expiration to 1 hour later
            val expiration = Date(now.time + 60 * 60 * 1000)

            // Build JWT token with claims
            val token = Jwts.builder()
                .claim("email", loginRequest.username)
                .setNotBefore(now)
                .setExpiration(expiration)
                .signWith(SignatureAlgorithm.HS256, JWT_KEY.toByteArray())
                .compact()

            // Configure cookie options (adjust secure flag as needed)
            val cookie = Cookie("token", token).apply {
                isHttpOnly = true
                secure = false // change to true if running in production over HTTPS
                maxAge = 24 * 60 * 60  // 24 hours in seconds
                path = "/"
            }
            response.addCookie(cookie)

            // Return JSON with token
            return ResponseEntity.ok(mapOf("token" to token))
        } catch (ex: Exception) {
            // Log exception if necessary
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to generate token"))
        }

    }
}