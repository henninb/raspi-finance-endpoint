//package finance.configurations
//
//import org.springframework.context.annotation.Primary
//import org.springframework.jdbc.datasource.DriverManagerDataSource
//import org.springframework.orm.jpa.JpaTransactionManager
//import org.springframework.transaction.PlatformTransactionManager
//import org.apache.tomcat.jdbc.pool.DataSource;
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.context.annotation.Bean
//import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
//import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
//import java.util.Properties
//import org.springframework.context.annotation.Configuration
//import org.springframework.context.annotation.Profile
//
//@Profile("ora")
//@Configuration
//open class OracleConfig @Autowired constructor(private var dataSourceProperties: DataSourceProperties) {
//    @Bean
//    open fun entityManagerFactory(): LocalContainerEntityManagerFactoryBean {
//        val em = LocalContainerEntityManagerFactoryBean()
//        em.dataSource = dataSource()
//        em.setPackagesToScan(
//            "finance.repositories", "finance.domain"
//        )
//        val vendorAdapter = HibernateJpaVendorAdapter()
//        em.jpaVendorAdapter = vendorAdapter
//        return em
//    }
//
//    @Primary
//    @Bean
//    open fun dataSource(): DataSource {
//        val props = Properties()
//        props.setProperty("v\$session.program", "raspi-finance-endpoint")
//        val dataSource = DriverManagerDataSource()
//        dataSource.url = dataSourceProperties.url
//        dataSource.username = dataSourceProperties.username
//        dataSource.password = dataSourceProperties.password
//        dataSource.connectionProperties = props
//        val dataSourcePool = DataSource()
//        dataSourcePool.dataSource = dataSource
//        dataSourcePool.initialSize = 5
//        dataSourcePool.maxIdle = 20
//        return dataSourcePool
//    }
//
//    @Bean
//    open fun transactionManager(): PlatformTransactionManager {
//        val transactionManager = JpaTransactionManager()
//        transactionManager.entityManagerFactory = entityManagerFactory().getObject()
//        return transactionManager
//    }
//
//}