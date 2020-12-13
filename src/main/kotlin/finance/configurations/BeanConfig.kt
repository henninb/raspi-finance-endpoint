package finance.configurations

import io.micrometer.core.aop.TimedAspect
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

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
