//package finance.services
//
//import io.jsonwebtoken.JwtException
//import io.jsonwebtoken.Jwts
//import io.jsonwebtoken.SignatureAlgorithm
//import org.springframework.beans.factory.annotation.Value
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
//import org.springframework.security.core.Authentication
//import org.springframework.security.core.authority.SimpleGrantedAuthority
//import org.springframework.security.core.userdetails.UserDetails
//import org.springframework.stereotype.Service
//import java.util.*
//import javax.annotation.PostConstruct
//import javax.servlet.http.HttpServletRequest
//
//
//@Service
//class JwtTokenProviderService(myUserDetails: JwtUserDetailService) {
//    /**
//     * THIS IS NOT A SECURE PRACTICE! For simplicity, we are storing a static key here. Ideally, in a
//     * microservices environment, this key would be kept on a config-server.
//     */
////    @Value("\${security.jwt.token.secret-key:secret-key}")
////    private var secretKey: String? = null
//    private var secretKey: String = "my-secrete-key"
//
//    //@Value("\${security.jwt.token.expire-length:3600000}")
//    //private val validityInMilliseconds: Long = 3600000 // 1h
//    private val validityInMilliseconds: Long = 300000
//
//    private val myUserDetails: JwtUserDetailService
//    @PostConstruct
//    protected fun init() {
//        secretKey = Base64.getEncoder().encodeToString(secretKey.toByteArray())
//    }
//
//    fun createToken(username: String): String {
//        val claims = Jwts.claims().setSubject(username)
////        val appUserRoles: List<AppUserRole>
////        claims["auth"] =
////            appUserRoles.stream().map(Function<AppUserRole, SimpleGrantedAuthority> { s: AppUserRole ->
////                SimpleGrantedAuthority(
////                    s.getAuthority()
////                )
////            }).filter { obj: SimpleGrantedAuthority? ->
////                Objects.nonNull(
////                    obj
////                )
////            }.collect(Collectors.toList())
//        val now = Date()
//        val validity = Date(now.time + validityInMilliseconds)
//        return Jwts.builder()
//            .setClaims(claims)
//            .setIssuedAt(now)
//            .setExpiration(validity)
//            .signWith(SignatureAlgorithm.HS256, secretKey)
//            .compact()
//    }
//
//    fun getAuthentication(token: String?): Authentication {
//        val userDetails: UserDetails = myUserDetails.loadUserByUsername(getUsername(token))
//        return UsernamePasswordAuthenticationToken(userDetails, "", userDetails.authorities)
//    }
//
//    fun getUsername(token: String?): String {
//        return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).body.subject
//    }
//
//    fun resolveToken(req: HttpServletRequest): String? {
//        val bearerToken = req.getHeader("Authorization")
//        return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
//            bearerToken.substring(7)
//        } else null
//    }
//
//    fun validateToken(token: String?): Boolean {
//        return try {
//            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token)
//            true
//        } catch (e: JwtException) {
//            throw RuntimeException("Expired or invalid JWT token")
//            //throw new CustomException("Expired or invalid JWT token", HttpStatus.INTERNAL_SERVER_ERROR);
//        } catch (e: IllegalArgumentException) {
//            throw RuntimeException("Expired or invalid JWT token")
//        }
//    }
//
//    init {
//        this.myUserDetails = myUserDetails
//    }
//}
