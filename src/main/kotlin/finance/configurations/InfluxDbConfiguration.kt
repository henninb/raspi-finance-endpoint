package finance.configurations

import io.micrometer.core.instrument.Clock
import io.micrometer.influx.InfluxConfig
import io.micrometer.influx.InfluxMeterRegistry
import org.springframework.boot.actuate.autoconfigure.metrics.export.ConditionalOnEnabledMetricsExport
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import java.time.Duration

@Configuration
@ConditionalOnEnabledMetricsExport("influx")
@ConditionalOnProperty(name = ["management.metrics.export.influx.enabled"], havingValue = "true")
open class InfluxDbConfiguration {

    @Bean
    open fun influxConfig(environment: Environment): InfluxConfig {
        return object : InfluxConfig {
            override fun get(key: String): String? = null

            override fun enabled(): Boolean {
                return environment.getProperty("management.metrics.export.influx.enabled", Boolean::class.java, false)
            }

            override fun uri(): String {
                return environment.getProperty("management.metrics.export.influx.uri", "http://localhost:8086")
            }

            override fun db(): String {
                return environment.getProperty("management.metrics.export.influx.db", "metrics")
            }

            override fun userName(): String? {
                return environment.getProperty("management.metrics.export.influx.user-name")
            }

            override fun password(): String? {
                return environment.getProperty("management.metrics.export.influx.password")
            }

            override fun autoCreateDb(): Boolean {
                return environment.getProperty("management.metrics.export.influx.auto-create-db", Boolean::class.java, true)
            }

            override fun compressed(): Boolean {
                return environment.getProperty("management.metrics.export.influx.compressed", Boolean::class.java, true)
            }

            override fun step(): Duration {
                val stepString = environment.getProperty("management.metrics.export.influx.step", "1m")
                return Duration.parse("PT$stepString")
            }

            override fun connectTimeout(): Duration {
                val timeoutString = environment.getProperty("management.metrics.export.influx.connect-timeout", "10s")
                return Duration.parse("PT$timeoutString")
            }

            override fun readTimeout(): Duration {
                val timeoutString = environment.getProperty("management.metrics.export.influx.read-timeout", "30s")
                return Duration.parse("PT$timeoutString")
            }
        }
    }

    @Bean
    open fun influxMeterRegistry(influxConfig: InfluxConfig): InfluxMeterRegistry {
        return InfluxMeterRegistry(influxConfig, Clock.SYSTEM)
    }
}