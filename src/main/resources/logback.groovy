import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.filter.LevelFilter
import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.core.util.FileSize
import org.springframework.boot.logging.logback.ColorConverter
import ch.qos.logback.classic.encoder.PatternLayoutEncoder

statusListener(OnConsoleStatusListener)

def env = System.getenv()
String appName = env['APPNAME'] ?: 'app'
String springProfile = env['SPRING_PROFILES_ACTIVE'] ?: 'profile'
String logFilePath = env['LOGS'] ?: 'logs'

String logFileName = "${logFilePath}/${appName}-${springProfile}.log"
String hibernateFileName = "${logFilePath}/${appName}-${springProfile}-hibernate.log"
String flywayFileName = "${logFilePath}/${appName}-${springProfile}-flyway.log"
String errorFileName = "${logFilePath}/${appName}-${springProfile}-error.log"
String logArchiveFileName = "${logFilePath}/archive/${appName}-${springProfile}.%d{yyyy-MM-dd}.gz"
String hibernateArchiveFileName = "${logFilePath}/archive/${appName}-hibernate.%d{yyyy-MM-dd}.gz"
String flywayArchiveFileName = "${logFilePath}/archive/${appName}-flyway.%d{yyyy-MM-dd}.gz"
String errorArchiveFileName = "${logFilePath}/archive/${appName}-error.%d{yyyy-MM-dd}.gz"

conversionRule("clr", ColorConverter)

appender("fileAppender", RollingFileAppender) {
    file = logFileName
    encoder(PatternLayoutEncoder) {
        pattern = "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    }
    rollingPolicy(TimeBasedRollingPolicy) {
        totalSizeCap = "1MB"
        maxHistory = 20
        fileNamePattern = logArchiveFileName
    }
    triggeringPolicy(SizeBasedTriggeringPolicy) {
        maxFileSize = FileSize.valueOf('10MB')
    }
}

appender("hibernateFileAppender", RollingFileAppender) {
    file = hibernateFileName
    encoder(PatternLayoutEncoder) {
        pattern = "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    }
    rollingPolicy(TimeBasedRollingPolicy) {
        totalSizeCap = "1MB"
        maxHistory = 20
        fileNamePattern = hibernateArchiveFileName
    }
    triggeringPolicy(SizeBasedTriggeringPolicy) {
        maxFileSize = FileSize.valueOf('10MB')
    }

    //TODO: should I comment this out
    filter(LevelFilter) {
        level = TRACE
    }
}

appender("flywayFileAppender", RollingFileAppender) {
    file = flywayFileName
    encoder(PatternLayoutEncoder) {
        pattern = "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    }
    rollingPolicy(TimeBasedRollingPolicy) {
        totalSizeCap = "1MB"
        maxHistory = 20
        fileNamePattern = flywayArchiveFileName
    }
    triggeringPolicy(SizeBasedTriggeringPolicy) {
        maxFileSize = FileSize.valueOf('10MB')
    }
}

appender("errorFileAppender", RollingFileAppender) {
    file = errorFileName
    encoder(PatternLayoutEncoder) {
        pattern = "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    }
    rollingPolicy(TimeBasedRollingPolicy) {
        totalSizeCap = "1MB"
        maxHistory = 20
        fileNamePattern = errorArchiveFileName
    }
    triggeringPolicy(SizeBasedTriggeringPolicy) {
        maxFileSize = FileSize.valueOf('10MB')
    }

    filter(ThresholdFilter) {
        level = ERROR
    }
}

appender("consoleAppender", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = '%clr(%d{yyyy-MM-dd HH:mm:ss}){blue} %clr([%thread]){green} - %clr(%-5level){yellow} %clr(%-36.36logger{36}){cyan} - %clr(%msg){magenta}%n'
    }
}

//appender("asyncAppender", AsyncAppender) {
//    queueSize = 500
//    discardingThreshold = 0
//    includeCallerData = true
//    appenderRef('fileAppender')
//}

//***********************************
// Standard Appender
//***********************************
//def createStandardAppender(String appenderName, String fileName) {
//    def dir = logDirectory
//    def format = logFormat
//    println "Adding appender ${appenderName} with file name ${fileName} in directory ${dir}"
//    appender(appenderName, RollingFileAppender) {
//        file = "${dir}/${fileName}.log"
//        encoder(PatternLayoutEncoder) {
//            pattern = "$format"
//        }
//        rollingPolicy(FixedWindowRollingPolicy) {
//            maxIndex = 4
//            fileNamePattern = "${LOGS:-logs}/archive/${APPNAME:-app-test}.%d{yyyy-MM-dd}.%i.log.gz"
//        }
//        triggeringPolicy(SizeBasedTriggeringPolicy) {
//            maxFileSize = FileSize.valueOf('10MB')
//        }
//    }
//}

//logger('org.apache.http', INFO)
//logger('finance', INFO)

//root(WARN, ['errorFileAppender'])
//logger('debug-logger', TRACE, ['hibernateFileAppender'], false)
//logger('error-logger', ERROR, ['errorFileAppender'])


//logger('org.hibernate.type.descriptor.sql.BasicBinder', TRACE, ['hibernateFileAppender'], false)
logger('org.hibernate', TRACE, ['hibernateFileAppender'], false)
//logger('org.flyway', WARN, ['flywayFileAppender'], false)

root(INFO, ['consoleAppender', 'fileAppender', 'errorFileAppender'])
