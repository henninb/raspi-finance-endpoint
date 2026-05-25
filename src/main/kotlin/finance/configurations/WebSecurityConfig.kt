package finance.configurations

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.security.config.Customizer
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain


@Configuration
open class WebSecurityConfig( private val environment: Environment)  {


//    @Autowired
//    fun configureAuthentication(auth: AuthenticationManagerBuilder, passwordEncoder: PasswordEncoder) {
//        val username = environment.getProperty("spring.security.user.name")
//        val password = environment.getProperty("spring.security.user.password")
//
//        auth.inMemoryAuthentication()
//            .withUser(username)
//            .password(passwordEncoder.encode(password)) // Encode the password
//            .roles("USER")
//    }

//    @Bean
//    open fun passwordEncoder(): PasswordEncoder {
//        return BCryptPasswordEncoder()
//    }
//    @Bean
//    open fun configure(http: HttpSecurity) : SecurityFilterChain {
//        // TODO: bh enable csrf (cross site request forgery)
//        // TODO: bh how to enable basic auth
//        http
//            .csrf().disable()
//            .authorizeHttpRequests()
//            .anyRequest()
//            .authenticated()
//            //.and()
//            //.oauth2ResourceServer().jwt()
//            //.httpBasic()
//            .and()
//            .sessionManagement()
//            .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // the server will not send a JSESSIONID cookie
//
//        return http.build()
//    }

//    @Bean
//    @Throws(Exception::class)
//    open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
//        http.authorizeHttpRequests().requestMatchers("/**").hasRole("USER").and().formLogin()
//        return http.build()
//    }


    @Bean
    open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests()
            .requestMatchers("/**").permitAll() // Allow all requests without authentication
            .and()
            .csrf().disable() // Disable CSRF protection (only do this for trusted environments)
            .formLogin().disable() // Disable form login

        return http.build()
    }

    @Bean
    open fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

//    @Bean
//    fun userDetailsService(): UserDetailsService {
//        val user: UserDetails = User.withDefaultPasswordEncoder()
//            .username("user")
//            .password("password")
//            .roles("USER")
//            .build()
//        val admin: UserDetails = User.withDefaultPasswordEncoder()
//            .username("admin")
//            .password("password")
//            .roles("ADMIN", "USER")
//            .build()
//        return InMemoryUserDetailsManager(user, admin)
//    }

//    @Bean
//    fun userDetailsService(): InMemoryUserDetailsManager {
//        val user: UserDetails = User.withDefaultPasswordEncoder()
//            .username("user")
//            .password("password")
//            .roles("USER")
//            .build()
//        return InMemoryUserDetailsManager(user)
//    }

//    @Bean
//    open fun configure(http: HttpSecurity): SecurityFilterChain {
//        http
//            .csrf().disable() // Disable CSRF protection (only do this for trusted environments)
//            .authorizeHttpRequests()
//            .antMatchers("/**").permitAll() // Allow all requests without authentication
//            .and()
//            .sessionManagement()
//            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//
//        return http.build()
//    }
}
