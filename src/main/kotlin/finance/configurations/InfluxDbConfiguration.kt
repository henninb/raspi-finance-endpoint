package finance.configurations

import io.micrometer.core.instrument.Clock
import io.micrometer.influx.InfluxConfig
import io.micrometer.influx.InfluxMeterRegistry
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import java.time.Duration

@Configuration
// TODO: ConditionalOnEnabledMetricsExport may have changed in Spring Boot 4.0.0-M1
// @ConditionalOnEnabledMetricsExport("influx")
@ConditionalOnProperty(name = ["management.metrics.export.influx.enabled"], havingValue = "true")
open class InfluxDbConfiguration {
    @Bean
    open fun influxConfig(environment: Environment): InfluxConfig = CustomInfluxConfig(environment)

    class CustomInfluxConfig(
        private val environment: Environment,
    ) : InfluxConfig {
        override fun get(key: String): String? {
            // Return property values for Micrometer's internal use
            return when (key) {
                "apiVersion" -> environment.getProperty("management.metrics.export.influx.api-version", "v1")
                else -> null
            }
        }

        override fun enabled(): Boolean = environment.getProperty("management.metrics.export.influx.enabled", Boolean::class.java, false)

        override fun uri(): String = environment.getProperty("management.metrics.export.influx.uri", "http://localhost:8086")

        override fun db(): String = environment.getProperty("management.metrics.export.influx.db", "metrics")

        override fun userName(): String? {
            val username = environment.getProperty("management.metrics.export.influx.user-name")
            return if (username.isNullOrBlank()) null else username
        }

        override fun password(): String? {
            val password = environment.getProperty("management.metrics.export.influx.password")
            return if (password.isNullOrBlank()) null else password
        }

        override fun autoCreateDb(): Boolean = environment.getProperty("management.metrics.export.influx.auto-create-db", Boolean::class.java, true)

        override fun compressed(): Boolean = environment.getProperty("management.metrics.export.influx.compressed", Boolean::class.java, true)

        override fun step(): Duration {
            val stepString = environment.getProperty("management.metrics.export.influx.step", "1m")
            return Duration.parse("PT$stepString")
        }

        @Deprecated("Deprecated in Micrometer InfluxConfig")
        override fun connectTimeout(): Duration {
            val timeoutString = environment.getProperty("management.metrics.export.influx.connect-timeout", "10s")
            return Duration.parse("PT$timeoutString")
        }

        @Deprecated("Deprecated in Micrometer InfluxConfig")
        override fun readTimeout(): Duration {
            val timeoutString = environment.getProperty("management.metrics.export.influx.read-timeout", "30s")
            return Duration.parse("PT$timeoutString")
        }

        override fun bucket(): String = environment.getProperty("management.metrics.export.influx.bucket", "")

        override fun org(): String? = environment.getProperty("management.metrics.export.influx.org")

        override fun token(): String? = environment.getProperty("management.metrics.export.influx.token")
    }

    @Bean
    open fun influxMeterRegistry(influxConfig: InfluxConfig): InfluxMeterRegistry = InfluxMeterRegistry(influxConfig, Clock.SYSTEM)
}
