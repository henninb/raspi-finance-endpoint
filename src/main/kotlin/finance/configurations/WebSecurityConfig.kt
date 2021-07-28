//package finance.configurations
//
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.Configuration
//import org.springframework.security.config.annotation.web.builders.HttpSecurity
//import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
//import org.springframework.web.cors.CorsConfiguration
//import org.springframework.web.cors.CorsConfigurationSource
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource
//
//
//@Configuration
//open class WebSecurityConfig : WebSecurityConfigurerAdapter() {
//    // TODO: disable security temporarily
//    @Throws(java.lang.Exception::class)
//    override fun configure(http: HttpSecurity) {
//
//
////        http.httpBasic();
////        http.csrf().ignoringAntMatchers("/account-holder");
////        http.csrf().ignoringAntMatchers("/admin");
////        http.csrf().ignoringAntMatchers("/admin/create/customer");
////        http.csrf().ignoringAntMatchers("/admin/modify");
////        http.csrf().ignoringAntMatchers("/balance/{id}");
////        http.csrf().ignoringAntMatchers("/balance/all");
////        http.csrf().ignoringAntMatchers("/checking/new");
////        http.csrf().ignoringAntMatchers("/saving/new");
////        http.csrf().ignoringAntMatchers("/credit-card/new");
////        http.csrf().ignoringAntMatchers("/transaction/new");
////
////
////        http.authorizeRequests()
////            .mvcMatchers(HttpMethod.GET, "/account-holder").hasRole("ADMIN")
////            .mvcMatchers(HttpMethod.POST, "/account-holder").hasRole("ADMIN")
////            .mvcMatchers(HttpMethod.POST, "/admin/create/customer").hasRole("ADMIN")
////            .mvcMatchers(HttpMethod.PATCH, "/admin/modify").hasRole("ADMIN")
////            .mvcMatchers(HttpMethod.GET, "/balance/{id}").hasRole("ACCOUNT_HOLDER")
////            .mvcMatchers(HttpMethod.GET, "/balance/all").hasAnyRole("ACCOUNT_HOLDER", "THIRD_PARTY")
////            .mvcMatchers(HttpMethod.GET, "/checking/all").hasRole("ADMIN")
////            .mvcMatchers(HttpMethod.POST, "/checking/new").hasRole("ADMIN")
////            .mvcMatchers(HttpMethod.GET, "/credit-card/all").hasRole("ADMIN")
////            .mvcMatchers(HttpMethod.POST, "/credit-card/new").hasRole("ADMIN")
////            .mvcMatchers(HttpMethod.GET, "/saving/all").hasRole("ADMIN")
////            .mvcMatchers(HttpMethod.POST, "/saving/new").hasRole("ADMIN")
////            .mvcMatchers(HttpMethod.GET, "/student/all").hasRole("ADMIN")
////            .mvcMatchers(HttpMethod.GET, "/transaction/all").hasRole("ADMIN")
////            .mvcMatchers(HttpMethod.POST, "/transaction/new").hasRole("ACCOUNT_HOLDER")
////            .mvcMatchers(HttpMethod.POST, "/transaction/tpu/to").hasRole("ACCOUNT_HOLDER")
////            .mvcMatchers(HttpMethod.POST, "/transaction/tpu/from").hasRole("THIRD_PARTY")
////            .mvcMatchers(HttpMethod.POST, "/admin/create/third-party").hasRole("ADMIN")
////            .anyRequest().permitAll();
//
////        http.csrf().ignoringAntMatchers("/createAccount");
////        http.csrf().ignoringAntMatchers("/login");
////        http.csrf().ignoringAntMatchers("/upload");
////        http.csrf().ignoringAntMatchers("/profilePic");
////        http.csrf().ignoringAntMatchers("/offlineProfile/*");
////        http.csrf().ignoringAntMatchers("/resetPassword");
////
////        http
////            .authorizeRequests()
////            .antMatchers("/createAccount").permitAll()
////            .antMatchers("/profilePic").permitAll()
////            .antMatchers("/offlineProfile/*").permitAll()
////            .antMatchers("/resetPassword").permitAll()
//
//        http.cors()//.and().csrf().disable()
//            //.and().authorizeRequests()//.antMatchers("/graphql").permitAll()
//    }
//
//    @Bean
//    open fun corsConfigurationSource(): CorsConfigurationSource {
//        val corsConfiguration = CorsConfiguration()
//        corsConfiguration.allowedOrigins = mutableListOf(
//            "https://hornsup:3000",
//            "https://localhost:3000",
//            "http://localhost:3000",
//            "http://hornsup:3000"
//        )
//        corsConfiguration.allowedMethods = mutableListOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
//        corsConfiguration.allowedHeaders = mutableListOf("*")
//        val source = UrlBasedCorsConfigurationSource()
//        source.registerCorsConfiguration("/**", corsConfiguration)
//        return source
//    }
//}
//
////@Configuration
////class CorsFilterConfigurer : WebFluxConfigurer {
////
////    override fun addCorsMappings(registry: CorsRegistry) {
////        registry.addMapping("/**")
////            .allowedOrigins("*")
////            .allowedMethods("*")
////            .allowedHeaders("*")
////            .exposedHeaders("Access-Control-Allow-Origin",
////                "Access-Control-Allow-Methods",
////                "Access-Control-Allow-Headers",
////                "Access-Control-Max-Age",
////                "Access-Control-Request-Headers",
////                "Access-Control-Request-Method")
////            .maxAge(3600)
////
////    }
////}