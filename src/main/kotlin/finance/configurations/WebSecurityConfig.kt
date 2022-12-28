package finance.configurations

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration


@Configuration
open class WebSecurityConfig()  {
    @Bean
    @Throws(Exception::class)
    open fun configure(http: HttpSecurity) : SecurityFilterChain {
        val corsConfiguration = CorsConfiguration()

        //corsConfiguration.allowedOrigins = mutableListOf("http://localhost", "https://localhost:3000", "https://localhost")
        corsConfiguration.allowedOriginPatterns = mutableListOf("*")
        corsConfiguration.allowedMethods = mutableListOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        //corsConfiguration.allowedHeaders = mutableListOf("*")
        corsConfiguration.allowCredentials = true // this line is important it sends only specified domain instead of *
        corsConfiguration.exposedHeaders = mutableListOf("x-auth-token")
        corsConfiguration.allowedHeaders = mutableListOf("authorization", "content-type", "x-auth-token", "accept");

        // No session will be created or used by spring security
        // http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)

        // Entry points
//        http.authorizeRequests() //
//            .requestMatchers("/user/signin").permitAll()
//            .requestMatchers("/user/signup").permitAll()
//            // Disallow everything else
//            .anyRequest().authenticated()

        // TODO: bh enable csrf (cross site request forgery)
        // TODO: bh enable headers (not sure what happens when they are disabled) preflight
        http.httpBasic().and()
            .cors()
            .configurationSource { corsConfiguration }.and()
            .headers().disable()
            .csrf().disable()
        return http.build()
    }
}
