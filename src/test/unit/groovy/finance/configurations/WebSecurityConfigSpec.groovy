package finance.configurations

import finance.services.TokenBlacklistService
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
    TokenBlacklistService tokenBlacklistService = Mock()

    def setup() {
        webSecurityConfig = new WebSecurityConfig(environment, 'test_jwt_key_for_web_security_config_spec_test_jwt_key_x')
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

    def "should create jwt authentication filter bean with meter registry and token blacklist service"() {
        when:
        JwtAuthenticationFilter filter = webSecurityConfig.jwtAuthenticationFilter(meterRegistry, tokenBlacklistService, new CustomProperties())

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

    def "validateSecurityConfiguration passes for valid JWT key length"() {
        given:
        def config = new WebSecurityConfig(environment, 'a' * 64)
        environment.getActiveProfiles() >> []

        when:
        config.validateSecurityConfiguration()

        then:
        noExceptionThrown()
    }

    def "validateSecurityConfiguration throws for JWT key shorter than 32 bytes"() {
        given:
        def config = new WebSecurityConfig(environment, 'short')
        environment.getActiveProfiles() >> []

        when:
        config.validateSecurityConfiguration()

        then:
        thrown(IllegalStateException)
    }

    def "validateSecurityConfiguration passes for prod profile with SSL in datasource URL"() {
        given:
        def config = new WebSecurityConfig(environment, 'a' * 64)
        environment.getActiveProfiles() >> ['prod']
        setPrivateField(config, 'datasourceUrl', 'jdbc:postgresql://host:5432/db?sslmode=require')

        when:
        config.validateSecurityConfiguration()

        then:
        noExceptionThrown()
    }

    def "validateSecurityConfiguration throws for prod profile without SSL in datasource URL"() {
        given:
        def config = new WebSecurityConfig(environment, 'a' * 64)
        environment.getActiveProfiles() >> ['prod']
        setPrivateField(config, 'datasourceUrl', 'jdbc:postgresql://host:5432/db')

        when:
        config.validateSecurityConfiguration()

        then:
        thrown(IllegalStateException)
    }

    def "validateSecurityConfiguration skips SSL check for prod profile with blank datasource URL"() {
        given:
        def config = new WebSecurityConfig(environment, 'a' * 64)
        environment.getActiveProfiles() >> ['prod']
        setPrivateField(config, 'datasourceUrl', '')

        when:
        config.validateSecurityConfiguration()

        then:
        noExceptionThrown()
    }

    def "validateSecurityConfiguration passes for stage profile with SSL in datasource URL"() {
        given:
        def config = new WebSecurityConfig(environment, 'a' * 64)
        environment.getActiveProfiles() >> ['stage']
        setPrivateField(config, 'datasourceUrl', 'jdbc:postgresql://host:5432/db?ssl=true')

        when:
        config.validateSecurityConfiguration()

        then:
        noExceptionThrown()
    }

    def "validateSecurityConfiguration skips SSL check for non-prod profile"() {
        given:
        def config = new WebSecurityConfig(environment, 'a' * 64)
        environment.getActiveProfiles() >> ['unit']
        setPrivateField(config, 'datasourceUrl', 'jdbc:postgresql://host:5432/db')

        when:
        config.validateSecurityConfiguration()

        then:
        noExceptionThrown()
    }

    private static void setPrivateField(Object target, String fieldName, Object value) {
        def field = target.class.getDeclaredField(fieldName)
        field.accessible = true
        field.set(target, value)
    }

    def "should create filter registration beans with disabled status"() {
        given:
        def jwtFilter = new JwtAuthenticationFilter(meterRegistry, tokenBlacklistService, 'test_jwt_key_for_web_security_config_spec_test_jwt_key_x', new CustomProperties())
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