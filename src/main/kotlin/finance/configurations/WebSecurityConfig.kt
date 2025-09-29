package finance.configurations

import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
open class WebSecurityConfig(
    private val environment: Environment,
) {
    companion object {
        private val securityLogger = LoggerFactory.getLogger("SECURITY.${WebSecurityConfig::class.java.simpleName}")
    }

    @Bean
    @org.springframework.context.annotation.Profile("!func")
    open fun securityFilterChain(
        http: HttpSecurity,
        loggingCorsFilter: LoggingCorsFilter,
        httpErrorLoggingFilter: HttpErrorLoggingFilter,
        rateLimitingFilter: RateLimitingFilter,
        securityAuditFilter: SecurityAuditFilter,
        jwtAuthenticationFilter: JwtAuthenticationFilter,
    ): SecurityFilterChain {
        val activeProfiles = environment.activeProfiles.joinToString(",").ifBlank { "default" }
        securityLogger.info("SECURITY_CONFIG building main chain profiles={} stateless=true jwtFilter=true", activeProfiles)
        http
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(securityAuditFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(httpErrorLoggingFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(loggingCorsFilter, UsernamePasswordAuthenticationFilter::class.java)
            .cors { cors -> cors.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/api/login", "/api/register").permitAll()
                auth.requestMatchers("/graphql").authenticated()
                auth.requestMatchers("/api/**").authenticated()
                auth.requestMatchers("/account/**", "/category/**", "/description/**", "/parameter/**").authenticated()
                auth.anyRequest().permitAll()
            }
            .formLogin { it.disable() }
            .sessionManagement { session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.access.intercept.AuthorizationFilter::class.java)
        val chain = http.build()
        securityLogger.info("SECURITY_CONFIG built main chain: protected=['/api/**','/account/**','/category/**','/description/**','/parameter/**'] permit=['/api/login','/api/register']")
        return chain
    }

    @Bean
    open fun rateLimitingFilter(): RateLimitingFilter = RateLimitingFilter()

    @Bean
    open fun securityAuditFilter(meterRegistry: io.micrometer.core.instrument.MeterRegistry): SecurityAuditFilter = SecurityAuditFilter(meterRegistry)

    @Bean
    open fun httpErrorLoggingFilter(meterRegistry: io.micrometer.core.instrument.MeterRegistry): HttpErrorLoggingFilter = HttpErrorLoggingFilter(meterRegistry)

    @Bean
    open fun loggingCorsFilter(corsConfigurationSource: CorsConfigurationSource): LoggingCorsFilter = LoggingCorsFilter(corsConfigurationSource)

    @Bean
    open fun jwtAuthenticationFilter(meterRegistry: MeterRegistry): JwtAuthenticationFilter = JwtAuthenticationFilter(meterRegistry)

    // Prevent Boot from auto-registering these filters with the servlet container; they are managed by SecurityFilterChain
    @Bean
    open fun jwtFilterRegistration(jwtAuthenticationFilter: JwtAuthenticationFilter): FilterRegistrationBean<JwtAuthenticationFilter> =
        FilterRegistrationBean<JwtAuthenticationFilter>(jwtAuthenticationFilter).apply { isEnabled = false }

    @Bean
    open fun rateLimitFilterRegistration(rateLimitingFilter: RateLimitingFilter): FilterRegistrationBean<RateLimitingFilter> =
        FilterRegistrationBean<RateLimitingFilter>(rateLimitingFilter).apply { isEnabled = false }

    @Bean
    open fun securityAuditFilterRegistration(securityAuditFilter: SecurityAuditFilter): FilterRegistrationBean<SecurityAuditFilter> =
        FilterRegistrationBean<SecurityAuditFilter>(securityAuditFilter).apply { isEnabled = false }

    @Bean
    open fun httpErrorLoggingFilterRegistration(httpErrorLoggingFilter: HttpErrorLoggingFilter): FilterRegistrationBean<HttpErrorLoggingFilter> =
        FilterRegistrationBean<HttpErrorLoggingFilter>(httpErrorLoggingFilter).apply { isEnabled = false }

    @Bean
    open fun loggingCorsFilterRegistration(loggingCorsFilter: LoggingCorsFilter): FilterRegistrationBean<LoggingCorsFilter> =
        FilterRegistrationBean<LoggingCorsFilter>(loggingCorsFilter).apply { isEnabled = false }

    @Bean
    open fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration =
            CorsConfiguration().apply {
                allowedOrigins =
                    listOf(
                        "http://localhost:3000",
                        "https://www.bhenning.com",
                        "https://www.brianhenning.com",
                        "https://vercel.bhenning.com",
                        "https://vercel.brianhenning.com",
                        "https://pages.brianhenning.com",
                        "https://pages.bhenning.com",
                        "https://amplify.bhenning.com",
                        "https://amplify.brianhenning.com",
                        "https://netlify.bhenning.com",
                        "https://netlify.brianhenning.com",
                        "https://finance.bhenning.com",
                        "https://finance.brianhenning.com",
                        "http://dev.finance.bhenning.com:3000",
                        "http://dev.finance.bhenning.com:3001",
                        "chrome-extension://ldehlkfgenjholjmakdlmgbchmebdinc",
                    )
                allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
                allowedHeaders = listOf("Content-Type", "Accept", "Cookie", "X-Requested-With", "Authorization", "X-Api-Key")
                allowCredentials = true
                // Spring Framework version here expects seconds as Long
                maxAge = 3600L
            }
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    open fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
