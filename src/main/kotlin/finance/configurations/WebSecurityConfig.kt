package finance.configurations

import finance.services.TokenBlacklistService
import io.micrometer.core.instrument.MeterRegistry
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
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
    @Value("\${custom.project.jwt.key}") private val jwtKey: String,
) {
    @Value("\${custom.project.chrome-extension-id:}")
    private var chromeExtensionId: String = ""

    @Value("\${spring.datasource.url:}")
    private var datasourceUrl: String = ""

    companion object {
        private val securityLogger = LoggerFactory.getLogger("SECURITY.${WebSecurityConfig::class.java.simpleName}")
        private const val MIN_JWT_KEY_BYTES = 32
        private val PROD_PROFILES = setOf("prod", "production", "stage", "prodora")
    }

    @PostConstruct
    fun validateSecurityConfiguration() {
        val keyBytes = jwtKey.toByteArray(Charsets.UTF_8).size
        check(keyBytes >= MIN_JWT_KEY_BYTES) {
            "FATAL: custom.project.jwt.key is $keyBytes bytes — minimum $MIN_JWT_KEY_BYTES bytes (256 bits) required for HS256"
        }
        securityLogger.info("SECURITY_CONFIG JWT key validated: {} bytes", keyBytes)

        val isProd = environment.activeProfiles.any { it.lowercase() in PROD_PROFILES }
        if (isProd && datasourceUrl.isNotBlank()) {
            val urlLower = datasourceUrl.lowercase()
            check(urlLower.contains("ssl") || urlLower.contains("sslmode")) {
                "FATAL: DATASOURCE URL must include SSL parameters in production (e.g., ?sslmode=require). Current URL does not contain 'ssl'."
            }
            securityLogger.info("SECURITY_CONFIG datasource SSL parameter present in URL")
        }
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
                headers.cacheControl { }
                headers.contentTypeOptions { }
                headers.frameOptions { it.deny() }
                headers.referrerPolicy { it.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN) }
                // Apply HSTS for all internet-facing profiles
                val isProd =
                    environment.activeProfiles.any {
                        it.equals("prod", true) || it.equals("production", true) ||
                            it.equals("stage", true) || it.equals("prodora", true)
                    }
                if (isProd) {
                    headers.httpStrictTransportSecurity { hsts ->
                        hsts.includeSubDomains(true).preload(true).maxAgeInSeconds(31536000)
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
            // 4. Exemptions: login/register (pre-authentication) only
            // /graphql is NOT exempted — mutations require X-CSRF-TOKEN header
            // This provides defense-in-depth against CSRF attacks and resolves CodeQL alert #10
            .csrf { csrf ->
                val csrfTokenRepository =
                    CookieCsrfTokenRepository
                        .withHttpOnlyFalse()
                        .apply {
                            setHeaderName("X-CSRF-TOKEN")
                        }
                csrf
                    .csrfTokenRepository(csrfTokenRepository)
                    .csrfTokenRequestHandler(SpaCsrfTokenRequestHandler())
                    .ignoringRequestMatchers("/api/login", "/api/register", "/graphiql")
            }.authorizeHttpRequests { auth ->
                auth.requestMatchers("/api/login", "/api/register", "/api/logout", "/api/csrf").permitAll()
                auth.requestMatchers("/graphiql").permitAll()
                auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").authenticated()
                auth.requestMatchers("/actuator/health", "/health").permitAll()
                auth.requestMatchers("/graphql").authenticated()
                auth.requestMatchers("/api/**").authenticated()
                auth.requestMatchers("/performance/**").authenticated()
                auth.requestMatchers("/actuator/**").authenticated()
                auth.anyRequest().denyAll()
            }.formLogin { it.disable() }
            .httpBasic { it.disable() }
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
        customProperties: CustomProperties,
    ): JwtAuthenticationFilter = JwtAuthenticationFilter(meterRegistry, tokenBlacklistService, jwtKey, customProperties)

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
                val origins =
                    mutableListOf(
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
                        "https://dev.finance.bhenning.com:3000",
                        "https://dev.finance.bhenning.com:3001",
                    )
                if (chromeExtensionId.isNotBlank()) {
                    origins.add("chrome-extension://$chromeExtensionId")
                }
                allowedOrigins = origins
                allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
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
