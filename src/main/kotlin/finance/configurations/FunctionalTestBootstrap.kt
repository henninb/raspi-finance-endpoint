package finance.configurations

import jakarta.annotation.PostConstruct
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import javax.sql.DataSource

@Configuration(proxyBeanMethods = false)
@Profile("func")
open class FunctionalTestBootstrap(
    private val flyway: Flyway?,
    private val dataSource: DataSource,
) {
    private val log = LoggerFactory.getLogger(FunctionalTestBootstrap::class.java)

    @PostConstruct
    open fun init() {
        try {
            if (flyway != null) {
                flyway.migrate()
                log.info("Functional profile: Flyway migrations applied at startup (bean)")
                return
            }
            try {
                val fw =
                    Flyway.configure()
                        .dataSource(dataSource)
                        .locations("classpath:db/migration/func")
                        .schemas("func")
                        .baselineOnMigrate(true)
                        .load()
                fw.migrate()
                log.info("Functional profile: Flyway migrations applied at startup (programmatic)")
            } catch (ex2: Exception) {
                log.warn("Functional profile: Programmatic Flyway migrate failed: ${ex2.message}")
            }
        } catch (ex: Exception) {
            log.warn("Functional profile: Flyway migrate skipped or failed: ${ex.message}")
        }
    }
}
