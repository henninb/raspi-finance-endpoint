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
    private val JWT_KEY = "Ei,a/_-y,5ZTn7rR*0DA@NK[rFX_L:!0hG+U@{2)k/7S2jN=6UJb%vp{.X.].N}:y*cR,R1D=!B{eY_E8CzYMNFE=q_+q!?eD4*kgwU.hnWBNSB{iEm=3DJMhzL}Lh(1Py%6Yx7&QB-ueC?%ZcLuE_8=rXpZx8%Mfi[uwz2w8bT;??X%0PBMYnxFR/U+rK}A/)PycZE[)YH)!?73?rBZUq:j;2YzrgJu(dyAWE:U:ui/1n]#EZRgMpeRiHbWW+V2}gTLw*;m,MK[PH4*Vug)6e%g(*wh(-NmneR[=h2{(*{.5QhG%wjDD[bim25miKkBN[UHnyYFvYL,-#6!;4GSkw1T6EN&;3,Q0/,J+df;vf8L{%Q(%Pr+jjtp:aWxkmGj0a-x246J}+(D6NENN_iFuHKF74FQ}[/h:]Dt/}4!h,&wSX(?1L30v=jqJz%EX#$&)Ftd.SgPNGzeMUc=aZ,ty__H(,}ddkfxZdb/]Z@jeuT{D&U0F6@{en%Ej!:u3h9uP55#"

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