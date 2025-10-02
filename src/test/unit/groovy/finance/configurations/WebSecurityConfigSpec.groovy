package finance.configurations

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.core.env.Environment
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import spock.lang.Specification

class WebSecurityConfigSpec extends Specification {

    WebSecurityConfig webSecurityConfig
    Environment environment = Mock()
    MeterRegistry meterRegistry = Mock()

    def setup() {
        webSecurityConfig = new WebSecurityConfig(environment)
    }

    def "should create password encoder bean"() {
        when:
        PasswordEncoder encoder = webSecurityConfig.passwordEncoder()

        then:
        encoder instanceof BCryptPasswordEncoder
    }

    def "should create rate limiting filter bean"() {
        when:
        RateLimitingFilter filter = webSecurityConfig.rateLimitingFilter()

        then:
        filter != null
        filter instanceof RateLimitingFilter
    }

    def "should create security audit filter bean with meter registry"() {
        when:
        SecurityAuditFilter filter = webSecurityConfig.securityAuditFilter(meterRegistry)

        then:
        filter != null
        filter instanceof SecurityAuditFilter
    }

    def "should create http error logging filter bean with meter registry"() {
        when:
        HttpErrorLoggingFilter filter = webSecurityConfig.httpErrorLoggingFilter(meterRegistry)

        then:
        filter != null
        filter instanceof HttpErrorLoggingFilter
    }

    def "should create jwt authentication filter bean with meter registry"() {
        when:
        JwtAuthenticationFilter filter = webSecurityConfig.jwtAuthenticationFilter(meterRegistry)

        then:
        filter != null
        filter instanceof JwtAuthenticationFilter
    }

    def "should create CORS configuration source"() {
        when:
        CorsConfigurationSource source = webSecurityConfig.corsConfigurationSource()

        then:
        source != null
        source instanceof org.springframework.web.cors.UrlBasedCorsConfigurationSource
    }

    def "should create CORS configuration with logging filter"() {
        given:
        def corsConfigSource = webSecurityConfig.corsConfigurationSource()

        when:
        LoggingCorsFilter filter = webSecurityConfig.loggingCorsFilter(corsConfigSource)

        then:
        filter != null
        filter instanceof LoggingCorsFilter
    }

    def "should create filter registration beans with disabled status"() {
        given:
        def jwtFilter = new JwtAuthenticationFilter(meterRegistry)
        def rateLimitFilter = new RateLimitingFilter()
        def securityAuditFilter = new SecurityAuditFilter(meterRegistry)
        def httpErrorLoggingFilter = new HttpErrorLoggingFilter(meterRegistry)
        def corsConfigSource = webSecurityConfig.corsConfigurationSource()
        def loggingCorsFilter = new LoggingCorsFilter(corsConfigSource)

        when:
        def jwtRegistration = webSecurityConfig.jwtFilterRegistration(jwtFilter)
        def rateLimitRegistration = webSecurityConfig.rateLimitFilterRegistration(rateLimitFilter)
        def securityAuditRegistration = webSecurityConfig.securityAuditFilterRegistration(securityAuditFilter)
        def httpErrorRegistration = webSecurityConfig.httpErrorLoggingFilterRegistration(httpErrorLoggingFilter)
        def corsRegistration = webSecurityConfig.loggingCorsFilterRegistration(loggingCorsFilter)

        then:
        !jwtRegistration.enabled
        !rateLimitRegistration.enabled
        !securityAuditRegistration.enabled
        !httpErrorRegistration.enabled
        !corsRegistration.enabled
    }

}