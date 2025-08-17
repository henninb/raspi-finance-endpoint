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
            try {
                flyway.migrate()
            } catch (e: Exception) {
                if (e.message?.contains("Migrations have failed validation") == true) {
                    println("Flyway validation failed, attempting repair...")
                    flyway.repair()
                    println("Flyway repair completed, retrying migration...")
                    flyway.migrate()
                } else {
                    throw e
                }
            }
        }
    }

    @Bean
    open fun timedAspect(meterRegistry: MeterRegistry): TimedAspect {
        return TimedAspect(meterRegistry)
    }

//


//    open fun customersAuthenticationManager(): AuthenticationManager {
//        return label@ AuthenticationManager { authentication: Authentication? ->
//            if (isCustomer(authentication)) {
//                return@label UsernamePasswordAuthenticationToken()
//            }
//            throw UsernameNotFoundException()
//        }
//    }
}
