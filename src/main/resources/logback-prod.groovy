import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy
import ch.qos.logback.core.status.NopStatusListener
import net.logstash.logback.appender.LogstashTcpSocketAppender
import net.logstash.logback.composite.loggingevent.LoggingEventJsonProviders
import net.logstash.logback.encoder.LogstashEncoder
import org.springframework.boot.logging.logback.ColorConverter

statusListener(NopStatusListener)

def env = System.getenv()
String appName = env['APPNAME'] ?: 'app'
String springProfile = env['SPRING_PROFILES_ACTIVE'] ?: 'profile'
String logFilePath = env['LOGS'] ?: 'logs'
String logFileName = "${logFilePath}/${appName}-${springProfile}.log"
String logArchiveFileName = "${logFilePath}/archive/${appName}-${springProfile}"

conversionRule("clr", ColorConverter)

appender("fileAppender", RollingFileAppender) {
    file = logFileName
    append = true
    encoder(PatternLayoutEncoder) {
        pattern = "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    }
    rollingPolicy(SizeAndTimeBasedRollingPolicy) {
        totalSizeCap = '10MB'
        maxFileSize = '1MB'
        maxHistory = 10
        fileNamePattern = "${logArchiveFileName}-%d{yyyy-MM-dd}-%i.log.gz"
    }
    encoder(PatternLayoutEncoder) {
        pattern = "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    }
}

appender("consoleAppender", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = '%clr(%d{yyyy-MM-dd HH:mm:ss}){blue} %clr([%thread]){green} - %clr(%-5level){yellow} %clr(%-36.36logger{36}){cyan} - %clr(%msg){magenta}%n'
    }
}

appender("logstashAppender", LogstashTcpSocketAppender) {
    remoteHost = 'hornsup'
    port = 4560
    encoder(LogstashEncoder) {
        providers(LoggingEventJsonProviders) {
        }
    }
}

root(INFO, ['consoleAppender', 'fileAppender', 'logstashAppender'])
