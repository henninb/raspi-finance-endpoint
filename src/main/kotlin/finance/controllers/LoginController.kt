package finance.controllers

import finance.domain.User
import finance.services.UserService
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import jakarta.servlet.http.Cookie
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
            .signWith(SignatureAlgorithm.HS256, jwtKey.toByteArray())
            .compact()

        val cookie = ResponseCookie.from("token", token)
            .domain(".bhenning.com")  // Make the cookie available to all subdomains of bhenning.com
            .path("/")
            .maxAge(7 * 24 * 60 * 60) // optional: set expiry as needed
            .httpOnly(true)
            .secure(true)             // ensure true if you're using HTTPS
            .sameSite("None")         // necessary for cross-site cookie sharing
            .build()

//        val cookie = ResponseCookie.from("token", token)
//            .httpOnly(true)
//            .secure(true)
//            .maxAge(24 * 60 * 60)
//            .sameSite("None")  //needed for cross origin cookie
//            .path("/")
//            .build()
        response.addHeader("Set-Cookie", cookie.toString())

        // Return 204 No Content with no response body.
        return ResponseEntity.noContent().build()
    }



    @PostMapping("/register")
    fun register(
        @RequestBody newUser: User,
        response: HttpServletResponse
    ): ResponseEntity<Void> {
        try {
            // Attempt to register the new user
            userService.signUp(newUser.username, newUser.password)
        } catch (e: IllegalArgumentException) {
            // If the username already exists, return a 409 Conflict response
            logger.info("Username ${newUser.username} already exists.")
            return ResponseEntity.status(HttpStatus.CONFLICT).build()
        }

        // Auto-login after successful registration: generate a JWT token
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
            .sameSite("None")  // needed for cross-origin cookie
            .path("/")
            .build()
        response.addHeader("Set-Cookie", cookie.toString())

        // Return a CREATED response (201)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }


}