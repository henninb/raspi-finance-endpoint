package finance.configurations

import finance.services.JwtTokenService
import finance.services.TokenBlacklistService
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.core.env.Environment
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.cors.CorsConfigurationSource
import spock.lang.Specification

class WebSecurityConfigSpec extends Specification {

    WebSecurityConfig webSecurityConfig
    Environment environment = Mock()
    MeterRegistry meterRegistry = Mock()
    TokenBlacklistService tokenBlacklistService = Mock()

    private static final String TEST_JWT_KEY = 'test_jwt_key_for_web_security_config_spec_test_jwt_key_x'

    def setup() {
        webSecurityConfig = new WebSecurityConfig(environment, new CustomProperties())
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
        given:
        def jwtTokenService = new JwtTokenService(TEST_JWT_KEY, "dev")

        when:
        JwtAuthenticationFilter filter = webSecurityConfig.jwtAuthenticationFilter(meterRegistry, tokenBlacklistService, new CustomProperties(), jwtTokenService)

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

    def "validateSecurityConfiguration passes when datasource URL is blank in prod"() {
        given:
        environment.getActiveProfiles() >> ['prod']
        setPrivateField(webSecurityConfig, 'datasourceUrl', '')

        when:
        webSecurityConfig.validateSecurityConfiguration()

        then:
        noExceptionThrown()
    }

    def "validateSecurityConfiguration passes for prod profile with SSL in datasource URL"() {
        given:
        environment.getActiveProfiles() >> ['prod']
        setPrivateField(webSecurityConfig, 'datasourceUrl', 'jdbc:postgresql://host:5432/db?sslmode=require')

        when:
        webSecurityConfig.validateSecurityConfiguration()

        then:
        noExceptionThrown()
    }

    def "validateSecurityConfiguration throws for prod profile without SSL in datasource URL"() {
        given:
        environment.getActiveProfiles() >> ['prod']
        setPrivateField(webSecurityConfig, 'datasourceUrl', 'jdbc:postgresql://host:5432/db')

        when:
        webSecurityConfig.validateSecurityConfiguration()

        then:
        thrown(IllegalStateException)
    }

    def "validateSecurityConfiguration skips SSL check for prod profile with blank datasource URL"() {
        given:
        environment.getActiveProfiles() >> ['prod']
        setPrivateField(webSecurityConfig, 'datasourceUrl', '')

        when:
        webSecurityConfig.validateSecurityConfiguration()

        then:
        noExceptionThrown()
    }

    def "validateSecurityConfiguration passes for stage profile with SSL in datasource URL"() {
        given:
        environment.getActiveProfiles() >> ['stage']
        setPrivateField(webSecurityConfig, 'datasourceUrl', 'jdbc:postgresql://host:5432/db?ssl=true')

        when:
        webSecurityConfig.validateSecurityConfiguration()

        then:
        noExceptionThrown()
    }

    def "validateSecurityConfiguration skips SSL check for non-prod profile"() {
        given:
        environment.getActiveProfiles() >> ['unit']
        setPrivateField(webSecurityConfig, 'datasourceUrl', 'jdbc:postgresql://host:5432/db')

        when:
        webSecurityConfig.validateSecurityConfiguration()

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
        def jwtTokenService = new JwtTokenService(TEST_JWT_KEY, "dev")
        def jwtFilter = new JwtAuthenticationFilter(meterRegistry, tokenBlacklistService, jwtTokenService, new CustomProperties())
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
