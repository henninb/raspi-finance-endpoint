package finance.configurations

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
open class WebSecurityConfig( private val jwtAuthenticationFilter: JwtAuthenticationFilter ) {




    @Bean
    open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors(Customizer { cors ->
                cors.configurationSource(corsConfigurationSource())
            })
            .csrf { it.disable() } // Disable CSRF for trusted environments
            .authorizeHttpRequests { auth ->
                // Permit public endpoints like /api/login and /api/register while securing others as needed
                auth.requestMatchers("/api/login", "/api/register").permitAll()
                auth.requestMatchers("/**").permitAll() // Allow all requests without authentication
                    .anyRequest().authenticated()
            }
            .formLogin { it.disable() } // Disable form login
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }



//    @Bean
//    open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
//        http
//            .cors(Customizer { cors ->
//                cors.configurationSource(corsConfigurationSource())
//            })
//            .csrf { it.disable() } // Disable CSRF for trusted environments
//            .authorizeHttpRequests { auth ->
//                auth.requestMatchers("/**").permitAll() // Allow all requests without authentication
////                auth.requestMatchers("/api/login").permitAll() // Public endpoint for login
////                    .anyRequest().authenticated() // Secure all other endpoints
//            }
//            .formLogin { it.disable() } // Disable form login
//            .sessionManagement { session ->
//                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//            }
//            //.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
//        return http.build()
//    }


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
                "https://fianace.bhenning.com",
                "https://fianace.brianhenning.com",
                "finance.lan",
            )
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
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
}