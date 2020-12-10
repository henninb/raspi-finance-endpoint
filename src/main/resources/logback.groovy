//import ch.qos.logback.classic.encoder.PatternLayoutEncoder
//import ch.qos.logback.core.ConsoleAppender
//import ch.qos.logback.core.rolling.FixedWindowRollingPolicy
//import ch.qos.logback.core.rolling.RollingFileAppender
//import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy
import ch.qos.logback.core.util.FileSize
import org.springframework.boot.logging.logback.ColorConverter
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
//import ch.qos.logback.core.ConsoleAppender
//
//import static ch.qos.logback.classic.Level.INFO
//import static ch.qos.logback.classic.Level.WARN


//def MESSAGE_FORMAT = "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
//def consoleAppender = "CONSOLE"
def appName = System.getProperty('spring.application.name')

//String appName = config.app.name
//appName = Application.DEFAULT_APP_NAME

conversionRule("clr", ColorConverter)

appender("fileAppender", RollingFileAppender) {
    file = "/opt/raspi-finance-endpoint/logs/${appName}.log"
    encoder(PatternLayoutEncoder) {
        pattern = "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    }
    rollingPolicy(TimeBasedRollingPolicy) {
        totalSizeCap = "10MB"
        maxHistory = 30
        fileNamePattern = "/opt/raspi-finance-endpoint/logs/archive/${appName}.%d{yyyy-MM-dd}.gz"
    }
    triggeringPolicy(SizeBasedTriggeringPolicy) {
        maxFileSize = FileSize.valueOf('10MB')
    }
}

appender("consoleAppender", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = '%clr(%d{yyyy-MM-dd HH:mm:ss}){blue} %clr([%thread]){green} - %clr(%-5level){yellow} %clr(%-36.36logger{36}){cyan} - %clr(%msg){magenta}%n'
    }
}

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

logger('org.apache.http', INFO)

root(INFO, ['consoleAppender'])
root(INFO, ['fileAppender'])
