package finance.configurations

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.cors.CorsConfiguration

@Configuration
open class WebSecurityConfig : WebSecurityConfigurerAdapter() {
    @Throws(java.lang.Exception::class)
    override fun configure(http: HttpSecurity) {
        val corsConfiguration = CorsConfiguration()



        //corsConfiguration.allowedOrigins = mutableListOf("*")
        corsConfiguration.allowedOriginPatterns = mutableListOf("*")
        corsConfiguration.allowedMethods = mutableListOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        //corsConfiguration.allowedHeaders = mutableListOf("*")
        corsConfiguration.allowCredentials = true // this line is important it sends only specified domain instead of *
        corsConfiguration.exposedHeaders = mutableListOf("x-auth-token")
        corsConfiguration.allowedHeaders = mutableListOf("authorization", "content-type", "x-auth-token", "accept");

        // TODO: bh enable csrf (cross site request forgery)
        http.csrf().disable()

        // No session will be created or used by spring security
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)

//        // Entry points
//        http.authorizeRequests() //
//            .antMatchers("/user/signin").permitAll()
//            .antMatchers("/user/signup").permitAll()
//            // Disallow everything else
//            .anyRequest().authenticated()

        // TODO: bh enable headers (not sure what happens when they are disabled) preflight
        http.cors()
            .configurationSource { corsConfiguration }.and().headers().disable()

//        http.cors()
//            .configurationSource { corsConfiguration }

        http.httpBasic()

    }

//    @Bean
//    open fun passwordEncoder(): PasswordEncoder {
//        return BCryptPasswordEncoder(12)
//    }

}