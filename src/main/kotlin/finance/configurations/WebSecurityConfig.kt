package finance.configurations

import finance.services.TokenBlacklistService
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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy
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
            .headers { headers ->
                headers.contentTypeOptions { }
                headers.frameOptions { it.deny() }
                headers.referrerPolicy { it.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN) }
                // Apply HSTS only when running with a production-like profile
                val isProd = environment.activeProfiles.any { it.equals("prod", true) || it.equals("production", true) }
                if (isProd) {
                    headers.httpStrictTransportSecurity { hsts ->
                        hsts.includeSubDomains(true).preload(true).maxAgeInSeconds(15552000)
                    }
                }
                headers.contentSecurityPolicy { csp ->
                    // Minimal CSP for API-only service; adjust if serving web content
                    csp.policyDirectives("default-src 'none'")
                }
            }
            // CSRF Protection Strategy:
            // 1. Cookie-based CSRF tokens (double-submit pattern via CookieCsrfTokenRepository)
            // 2. SameSite=Strict cookies (browser-level protection in production)
            // 3. JWT authentication via HttpOnly cookies
            // 4. Exemptions: login/register (pre-authentication), GraphQL (for dev convenience)
            // This provides defense-in-depth against CSRF attacks and resolves CodeQL alert #10
            .csrf { csrf ->
                val csrfTokenRepository =
                    CookieCsrfTokenRepository
                        .withHttpOnlyFalse()
                        .apply {
                            // Use X-CSRF-TOKEN header (instead of default X-XSRF-TOKEN)
                            // This matches the standard CSRF header name used by most frameworks
                            setHeaderName("X-CSRF-TOKEN")
                        }
                csrf
                    .csrfTokenRepository(csrfTokenRepository)
                    .csrfTokenRequestHandler(SpaCsrfTokenRequestHandler())
                    .ignoringRequestMatchers("/api/login", "/api/register", "/graphiql", "/graphql")
            }.authorizeHttpRequests { auth ->
                auth.requestMatchers("/api/login", "/api/register", "/api/logout", "/api/csrf").permitAll()
                auth.requestMatchers("/graphiql").permitAll()
                auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                auth.requestMatchers("/actuator/health", "/health").permitAll()
                auth.requestMatchers("/graphql").authenticated()
                auth.requestMatchers("/api/**").authenticated()
                auth.requestMatchers("/performance/**").authenticated()
                auth.requestMatchers("/actuator/**").authenticated()
                auth.anyRequest().denyAll()
            }.formLogin { it.disable() }
            .sessionManagement { session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.access.intercept.AuthorizationFilter::class.java)
        val chain = http.build()
        securityLogger.info("SECURITY_CONFIG built main chain: protected=['/api/**','/performance/**','/actuator/**'] permit=['/api/login','/api/register','/api/logout','/api/csrf','/graphiql','/swagger-ui/**','/actuator/health'] default=denyAll")
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
    open fun jwtAuthenticationFilter(
        meterRegistry: MeterRegistry,
        tokenBlacklistService: TokenBlacklistService,
    ): JwtAuthenticationFilter = JwtAuthenticationFilter(meterRegistry, tokenBlacklistService)

    // Prevent Boot from auto-registering these filters with the servlet container; they are managed by SecurityFilterChain
    @Bean
    open fun jwtFilterRegistration(jwtAuthenticationFilter: JwtAuthenticationFilter): FilterRegistrationBean<JwtAuthenticationFilter> = FilterRegistrationBean<JwtAuthenticationFilter>(jwtAuthenticationFilter).apply { isEnabled = false }

    @Bean
    open fun rateLimitFilterRegistration(rateLimitingFilter: RateLimitingFilter): FilterRegistrationBean<RateLimitingFilter> = FilterRegistrationBean<RateLimitingFilter>(rateLimitingFilter).apply { isEnabled = false }

    @Bean
    open fun securityAuditFilterRegistration(securityAuditFilter: SecurityAuditFilter): FilterRegistrationBean<SecurityAuditFilter> = FilterRegistrationBean<SecurityAuditFilter>(securityAuditFilter).apply { isEnabled = false }

    @Bean
    open fun httpErrorLoggingFilterRegistration(httpErrorLoggingFilter: HttpErrorLoggingFilter): FilterRegistrationBean<HttpErrorLoggingFilter> = FilterRegistrationBean<HttpErrorLoggingFilter>(httpErrorLoggingFilter).apply { isEnabled = false }

    @Bean
    open fun loggingCorsFilterRegistration(loggingCorsFilter: LoggingCorsFilter): FilterRegistrationBean<LoggingCorsFilter> = FilterRegistrationBean<LoggingCorsFilter>(loggingCorsFilter).apply { isEnabled = false }

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
                allowedHeaders = listOf("Content-Type", "Accept", "Cookie", "X-Requested-With", "Authorization", "X-Api-Key", "X-CSRF-TOKEN")
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
