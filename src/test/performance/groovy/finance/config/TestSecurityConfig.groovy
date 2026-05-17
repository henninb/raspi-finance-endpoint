package finance.config

import finance.configurations.JwtAuthenticationFilter
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.intercept.AuthorizationFilter

@TestConfiguration(proxyBeanMethods = false)
@Profile("perf")
@Order(0)
class TestSecurityConfig {

    @Bean("perfTestSecurityFilterChain")
    @Order(0)
    SecurityFilterChain perfTestSecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {
        http
            .csrf { it.disable() }
            .anonymous { it.disable() }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/api/login", "/api/register").permitAll()
                auth.requestMatchers("/api/**").authenticated()
                auth.anyRequest().permitAll()
            }
            .formLogin { it.disable() }
            .sessionManagement { session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .addFilterBefore(jwtAuthenticationFilter, AuthorizationFilter.class)
        return http.build()
    }
}
