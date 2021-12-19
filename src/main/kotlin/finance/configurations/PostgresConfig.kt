package finance.configurations

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import java.util.*
import javax.persistence.EntityManagerFactory
import javax.sql.DataSource

@Configuration
open class PostgresConfig @Autowired constructor(private var dataSourceProperties: DataSourceProperties) {

    @Bean
    open fun dataSource(): DataSource {
        val properties = Properties()
        properties.setProperty("v\$session.program", "raspi-finance-endpoint")
        val dataSource = DriverManagerDataSource()
        dataSource.url = dataSourceProperties.url
        dataSource.username = dataSourceProperties.username
        dataSource.password = dataSourceProperties.password
        dataSource.connectionProperties = properties
        return dataSource
    }


//    @Bean
//    open fun entityManagerFactory(): LocalContainerEntityManagerFactoryBean {
//        val entityManager = LocalContainerEntityManagerFactoryBean()
//        entityManager.dataSource = dataSource()
//        entityManager.setPackagesToScan(
//            "finance.repositories", "finance.domain"
//        )
//        val vendorAdapter = HibernateJpaVendorAdapter()
//        entityManager.jpaVendorAdapter = vendorAdapter
//        return entityManager
//    }

    @Bean
    open fun entityManagerFactory(dataSource: DataSource): EntityManagerFactory? {
        val vendorAdapter = HibernateJpaVendorAdapter()
        vendorAdapter.setGenerateDdl(true)

        val entityManagerFactory = LocalContainerEntityManagerFactoryBean()
        entityManagerFactory.dataSource = dataSource
        entityManagerFactory.jpaVendorAdapter = vendorAdapter
        entityManagerFactory.setPackagesToScan("finance.repositories", "finance.domain")
        entityManagerFactory.afterPropertiesSet()
        return entityManagerFactory.getObject()
    }

//    @Bean
//    open fun transactionManager(entityManagerFactory: EntityManagerFactory?): JpaTransactionManager {
//        val jpaTransactionManager = JpaTransactionManager()
//        jpaTransactionManager.entityManagerFactory = entityManagerFactory
//        return jpaTransactionManager
//    }
//
//    @Bean
//    open fun exceptionTranslationPostProcessor(): PersistenceExceptionTranslationPostProcessor {
//        return PersistenceExceptionTranslationPostProcessor()
//    }
}