package finance.configurations

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy

/**
 * Central configuration for performance monitoring features.
 * Enables AOP, SQL logging, and metrics collection.
 */
@Configuration
@EnableAspectJAutoProxy
class PerformanceMonitoringConfiguration {
    /**
     * Configuration properties for performance monitoring
     */
    @Bean
    fun performanceMonitoringProperties(): PerformanceMonitoringProperties = PerformanceMonitoringProperties()

    data class PerformanceMonitoringProperties(
        // Method execution thresholds (in milliseconds)
        var methodWarnThresholdMs: Long = 500,
        var methodErrorThresholdMs: Long = 2000,
        // SQL query thresholds (in milliseconds)
        var sqlSlowQueryThresholdMs: Long = 100,
        var sqlVerySlowQueryThresholdMs: Long = 500,
        // Console reporting
        var consoleReportingEnabled: Boolean = true,
        var consoleReportingIntervalSeconds: Long = 60,
        // SQL logging
        var sqlLoggingEnabled: Boolean = true,
    )
}
