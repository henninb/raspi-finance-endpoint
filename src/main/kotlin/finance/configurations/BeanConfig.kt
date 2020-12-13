package finance.configurations


import io.micrometer.core.aop.TimedAspect
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.transaction.PlatformTransactionManager
// import org.apache.tomcat.jdbc.pool.DataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import java.util.Properties

@Configuration
open class BeanConfig {

    @Bean
    open fun migrationStrategy(): FlywayMigrationStrategy {
        return FlywayMigrationStrategy { flyway ->
            flyway.migrate()
        }
    }

    @Bean
    open fun timedAspect(registry: MeterRegistry): TimedAspect {
        return TimedAspect(registry)
    }

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
//        props.setProperty("v\$session.program", "finance-app")
//        val dataSource = DriverManagerDataSource()
//        dataSource.url = "jdbc:oracle:thin:@192.168.100.208:1521/ORCLCDB.localdomain"
//        dataSource.username = "henninb"
//        dataSource.password = "monday1"
//        dataSource.connectionProperties = props
//        //dataSource.setDriverClassName("oracle.jdbc.driver.OracleDriver")
//        //dataSource.setDriverClassName("oracle.jdbc.driver.OracleDriver")
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


//    @Bean
//    open fun servletContainerFactory(): EmbeddedServletContainerFactory {
//        val factory = TomcatEmbeddedServletContainerFactory()
//        factory.addConnectorCustomizers(TomcatConnectorCustomizer { connector ->
//            connector.setAttribute("sslProtocols", "TLSv1.1,TLSv1.2")
//            connector.setAttribute("sslEnabledProtocols", "TLSv1.1,TLSv1.2")
//        })
//        return factory
//    }


//    @Bean
//    open fun containerCustomizer() :  EmbeddedServletContainerCustomizer {
//        val container
//        return (container -> {
//            container.setPort(8080);
//        })
//    }

    //TODO: fix this bug
    //NioEndpoint - Error running socket processor, java.lang.NullPointerException
    //WORKAROUND: Disable TLSv1.3 and running with TLSv1.2 only. Or use OpenSSL for the encryption.
    //I simply changed the connector protocol from org.apache.coyote.http11.Http11Protocol to org.apache.coyote.http11.Http11NioProtocol
}
