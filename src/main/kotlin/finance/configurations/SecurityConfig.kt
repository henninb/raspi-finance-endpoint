package finance.configurations

import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

@Configuration
open class SecurityConfig : WebSecurityConfigurerAdapter() {
//    @Throws(Exception::class)
//    public override fun configure(http: HttpSecurity) {
//        http
//            .cors()
//            .and()
//            .authorizeRequests()
//            .anyRequest().permitAll()
//            .and().httpBasic()
//            .and()
//            .csrf()
//            .disable()
//        //in an attempt to fix the issue
//        //http.authorizeRequests().anyRequest().permitAll()
//        // http.authorizeRequests().anyRequest().authenticated()
//    }

    @Throws(java.lang.Exception::class)
    override fun configure(security: HttpSecurity) {
        security.httpBasic().disable()
    }
}