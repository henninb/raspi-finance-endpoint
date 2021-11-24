package finance.configurations

import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

@Configuration
open class SecurityConfig : WebSecurityConfigurerAdapter() {
    @Throws(Exception::class)
    public override fun configure(http: HttpSecurity) {
        http
            .cors()
            .and()
            .authorizeRequests()
            //.antMatchers("/").permitAll()
            //.antMatchers("/graphql").authenticated()
            .anyRequest().permitAll()
            .and().httpBasic()
//            .and()
//            .sessionManagement()
//            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .csrf()
            .disable()
//            .csrf().ignoringAntMatchers("/graphql")
        http.authorizeRequests().anyRequest().permitAll()
        // http.authorizeRequests().anyRequest().authenticated();
    }
}