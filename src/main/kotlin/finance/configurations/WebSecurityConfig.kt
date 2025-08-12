package finance.configurations

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.security.config.Customizer
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
open class WebSecurityConfig( private val jwtAuthenticationFilter: JwtAuthenticationFilter, private val environment: Environment ) {


    @Bean
    @Profile("!int") // Exclude when integration test profile is active
    open fun securityFilterChain(http: HttpSecurity, loggingCorsFilter: LoggingCorsFilter): SecurityFilterChain {
        http
            .addFilterBefore(loggingCorsFilter, UsernamePasswordAuthenticationFilter::class.java)
            .cors { cors ->
                cors.configurationSource(corsConfigurationSource())
            }
            .csrf { it.disable() } // Disable CSRF for trusted environments
            .authorizeHttpRequests { auth ->
                // Allow public access for login and registration endpoints
                auth.requestMatchers("/api/login", "/api/register", "/api/pending/transaction/insert").permitAll()
                // Protect all other API endpoints
                auth.requestMatchers("/api/**").authenticated()
            }
            .formLogin { it.disable() } // Disable form login
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    @Bean
    open fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = listOf(
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
                "chrome-extension://ldehlkfgenjholjmakdlmgbchmebdinc"
            )
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
            // Fixed: Explicit headers only - removed dangerous wildcard
            allowedHeaders = listOf(
                "Content-Type",
                "Accept",
                "Cookie",
                "X-Requested-With"
            )
            allowCredentials = true
        }
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    open fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean("intSecurityFilterChain")
    @Profile("int")
    open fun intSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                auth.anyRequest().permitAll() // Allow all requests without authentication for integration tests
            }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
        return http.build()
    }
}
