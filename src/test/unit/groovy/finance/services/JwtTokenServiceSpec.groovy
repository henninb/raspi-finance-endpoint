package finance.services

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import spock.lang.Specification

class JwtTokenServiceSpec extends Specification {

    private static final String VALID_KEY = 'a' * 64
    private static final String SHORT_KEY = 'short'

    def "init throws for JWT key shorter than 32 bytes"() {
        when:
        new JwtTokenService(SHORT_KEY, "dev")

        then:
        thrown(IllegalStateException)
    }

    def "init succeeds for JWT key of 32 bytes"() {
        when:
        new JwtTokenService('a' * 32, "dev")

        then:
        noExceptionThrown()
    }

    def "init succeeds for JWT key longer than 32 bytes"() {
        when:
        new JwtTokenService(VALID_KEY, "dev")

        then:
        noExceptionThrown()
    }

    def "buildToken produces a parseable JWT with correct claims"() {
        given:
        def service = new JwtTokenService(VALID_KEY, "dev")

        when:
        def token = service.buildToken("alice", false)
        def claims = service.parseClaims(token)

        then:
        claims.get(JwtTokenService.CLAIM_USERNAME, String) == "alice"
        claims.get(JwtTokenService.CLAIM_KEEP_LOGGED_IN, Boolean) == false
        claims.issuer == JwtTokenService.ISSUER
    }

    def "buildToken with keepLoggedIn=true uses long expiry"() {
        given:
        def service = new JwtTokenService(VALID_KEY, "dev")
        def before = System.currentTimeMillis()

        when:
        def token = service.buildToken("bob", true)
        def claims = service.parseClaims(token)

        then:
        def expiryMs = claims.expiration.time - before
        expiryMs > JwtTokenService.JWT_LONG_EXPIRY_MS - 5000
        expiryMs <= JwtTokenService.JWT_LONG_EXPIRY_MS + 5000
    }

    def "buildToken with keepLoggedIn=false uses short expiry"() {
        given:
        def service = new JwtTokenService(VALID_KEY, "dev")
        def before = System.currentTimeMillis()

        when:
        def token = service.buildToken("carol", false)
        def claims = service.parseClaims(token)

        then:
        def expiryMs = claims.expiration.time - before
        expiryMs > JwtTokenService.JWT_EXPIRY_MS - 5000
        expiryMs <= JwtTokenService.JWT_EXPIRY_MS + 5000
    }

    def "parseClaims throws JwtException for invalid token"() {
        given:
        def service = new JwtTokenService(VALID_KEY, "dev")

        when:
        service.parseClaims("not.a.valid.token")

        then:
        thrown(JwtException)
    }

    def "parseClaims throws for token signed with different key"() {
        given:
        def service = new JwtTokenService(VALID_KEY, "dev")
        def otherKey = Keys.hmacShaKeyFor(('b' * 64).bytes)
        def foreignToken = Jwts.builder()
            .issuer(JwtTokenService.ISSUER)
            .audience().add(JwtTokenService.AUDIENCE).and()
            .claim(JwtTokenService.CLAIM_USERNAME, "eve")
            .expiration(new Date(System.currentTimeMillis() + 3600_000L))
            .signWith(otherKey)
            .compact()

        when:
        service.parseClaims(foreignToken)

        then:
        thrown(JwtException)
    }

    def "extractToken returns token from Cookie array"() {
        given:
        def service = new JwtTokenService(VALID_KEY, "dev")
        def token = service.buildToken("dan", false)
        def req = Mock(HttpServletRequest)
        req.getCookies() >> ([new Cookie("token", token)] as Cookie[])

        when:
        def result = service.extractToken(req)

        then:
        result == token
    }

    def "extractToken returns token from Cookie header string"() {
        given:
        def service = new JwtTokenService(VALID_KEY, "dev")
        def token = "some.jwt.token"
        def req = Mock(HttpServletRequest)
        req.getCookies() >> null
        req.getHeader("Cookie") >> "other=x; token=${token}; another=y"

        when:
        def result = service.extractToken(req)

        then:
        result == token
    }

    def "extractToken returns token from Authorization Bearer header"() {
        given:
        def service = new JwtTokenService(VALID_KEY, "dev")
        def req = Mock(HttpServletRequest)
        req.getCookies() >> null
        req.getHeader("Cookie") >> null
        req.getHeader("Authorization") >> "Bearer mytoken123"

        when:
        def result = service.extractToken(req)

        then:
        result == "mytoken123"
    }

    def "extractToken returns null when no token present"() {
        given:
        def service = new JwtTokenService(VALID_KEY, "dev")
        def req = Mock(HttpServletRequest)
        req.getCookies() >> null
        req.getHeader("Cookie") >> null
        req.getHeader("Authorization") >> null

        when:
        def result = service.extractToken(req)

        then:
        result == null
    }

    def "buildTokenCookie sets SameSite=Lax for dev profile"() {
        given:
        def service = new JwtTokenService(VALID_KEY, "dev")
        def token = service.buildToken("frank", false)

        when:
        def cookie = service.buildTokenCookie(token, false)

        then:
        cookie.toString().contains("SameSite=Lax")
        cookie.toString().contains("HttpOnly")
        !cookie.toString().contains("Secure")
    }

    def "buildTokenCookie sets SameSite=Strict and Secure for prod profile"() {
        given:
        def service = new JwtTokenService(VALID_KEY, "prod")
        def token = service.buildToken("grace", false)

        when:
        def cookie = service.buildTokenCookie(token, false)

        then:
        cookie.toString().contains("SameSite=Strict")
        cookie.toString().contains("Secure")
    }

    def "buildClearCookie sets MaxAge=0"() {
        given:
        def service = new JwtTokenService(VALID_KEY, "dev")

        when:
        def cookie = service.buildClearCookie()

        then:
        cookie.toString().contains("Max-Age=0")
        cookie.value == ""
    }
}
