package finance.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.intercept.AuthorizationFilter
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

import finance.configurations.JwtAuthenticationFilter
import finance.configurations.LoggingCorsFilter
import finance.configurations.HttpErrorLoggingFilter
import finance.configurations.RateLimitingFilter
import finance.configurations.SecurityAuditFilter

@TestConfiguration(proxyBeanMethods = false)
@Profile("func")
@Order(0)
class TestSecurityConfig {

    @Bean("testSecurityFilterChain")
    @Order(0)
    SecurityFilterChain testSecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {
        http

            .csrf { it.disable() }
            .anonymous { it.disable() }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/api/login", "/api/register").permitAll()
                auth.requestMatchers("/graphql").authenticated()
                auth.requestMatchers("/api/**").authenticated()
                auth.anyRequest().permitAll()
            }
            .formLogin { it.disable() }
            .sessionManagement { session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .addFilterBefore(jwtAuthenticationFilter, AuthorizationFilter.class)

        return http.build()
    }
}
