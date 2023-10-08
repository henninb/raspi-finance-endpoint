package finance.configurations

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain


@Configuration
open class WebSecurityConfig( private val environment: Environment)  {


    @Autowired
    fun configureAuthentication(auth: AuthenticationManagerBuilder, passwordEncoder: PasswordEncoder) {
        val username = environment.getProperty("spring.security.user.name")
        val password = environment.getProperty("spring.security.user.password")

        auth.inMemoryAuthentication()
            .withUser(username)
            .password(passwordEncoder.encode(password)) // Encode the password
            .roles("USER")
    }
    @Bean
    open fun configure(http: HttpSecurity) : SecurityFilterChain {
//        val username = environment.getProperty("spring.security.user.name")
//        val password = environment.getProperty("spring.security.user.password")
//        println("U=$username")
//        println("P=${password}")

        // TODO: bh enable csrf (cross site request forgery)
        // TODO: bh how to enable basic auth
        http
            //.csrf().disable()
            .authorizeHttpRequests()
            .anyRequest()
            .authenticated()
            .and()
            //.oauth2ResourceServer().jwt()
            .httpBasic()
            .and()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // the server will not send a JSESSIONID cookie

        return http.build()
    }
}
