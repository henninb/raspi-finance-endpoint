//package finance.configs
//
//import org.springframework.context.annotation.Configuration
//import org.springframework.context.annotation.Profile
//import org.springframework.transaction.annotation.EnableTransactionManagement
//import org.h2.server.web.WebServlet
//import org.slf4j.LoggerFactory
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.context.annotation.Bean
//import org.springframework.jdbc.datasource.DriverManagerDataSource
//import javax.sql.DataSource
//import org.springframework.boot.web.servlet.ServletRegistrationBean
//import org.springframework.context.annotation.PropertySource
//import org.springframework.core.env.Environment
//import org.springframework.jdbc.core.JdbcTemplate
//
//@Configuration
//@EnableTransactionManagement
//@Profile("stage")
////@PropertySource("classpath:database-h2.properties")
//class H2Config {
//
//    private val logger = LoggerFactory.getLogger(this.javaClass)
//
//    @Autowired
//    lateinit var environment: Environment
//
//    private val url = "url"
//    private val username = "dbuser"
//    private val driver = "driver"
//    private val password = "dbpassword"
//
//    @Bean
//    open fun dataSource(): DataSource {
//        val dataSource = DriverManagerDataSource()
//        val driverName = environment.getProperty(driver)
//        if( driverName == null ) {
//            //TODO: need to take action
//            logger.info("driverName is NULL.")
//        } else {
//            dataSource.setDriverClassName(driverName)
//        }
//        dataSource.url = environment.getProperty(url)
//        dataSource.username = environment.getProperty(username)
//        dataSource.password = environment.getProperty(password)
//
//        //TODO: The block below is not working
//        //val initSchema = ClassPathResource("schema-h2.sql")
//        //val initData = ClassPathResource("data-h2.sql")
//        //val databasePopulator = ResourceDatabasePopulator(initSchema, initData)
//        //DatabasePopulatorUtils.execute(databasePopulator, dataSource)
//
//        return dataSource
//    }
//
//    @Bean
//    open fun h2servletRegistration(): ServletRegistrationBean<*> {
//        //return org.h2.tools.Server.createWebServer("-web","-webAllowOthers","-webDaemon","-webPort", "8080");
//        val registration = ServletRegistrationBean(WebServlet())
//        registration.addUrlMappings("/h2-console/*")
//        return registration
//    }
//
//    @Bean
//    open fun jdbcTemplate(dataSource: DataSource): JdbcTemplate {
//        return JdbcTemplate(dataSource)
//    }
//}
