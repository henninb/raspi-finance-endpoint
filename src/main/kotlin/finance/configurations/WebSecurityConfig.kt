package finance.configurations

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource


@Configuration
open class WebSecurityConfig : WebSecurityConfigurerAdapter() {
    // TODO: disable security temporarily
    @Throws(java.lang.Exception::class)
    override fun configure(http: HttpSecurity) {
        http.cors().and().csrf().disable()
    }

    @Bean
    open fun corsConfigurationSource(): CorsConfigurationSource {
        val corsConfiguration = CorsConfiguration()
        corsConfiguration.allowedOrigins = mutableListOf("*", "https://localhost:3000")
        corsConfiguration.allowedMethods = mutableListOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        corsConfiguration.allowedHeaders = mutableListOf("*")
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", corsConfiguration)
        return source
    }
}

//@Configuration
//class CorsFilterConfigurer : WebFluxConfigurer {
//
//    override fun addCorsMappings(registry: CorsRegistry) {
//        registry.addMapping("/**")
//            .allowedOrigins("*")
//            .allowedMethods("*")
//            .allowedHeaders("*")
//            .exposedHeaders("Access-Control-Allow-Origin",
//                "Access-Control-Allow-Methods",
//                "Access-Control-Allow-Headers",
//                "Access-Control-Max-Age",
//                "Access-Control-Request-Headers",
//                "Access-Control-Request-Method")
//            .maxAge(3600)
//
//    }
//}