package finance

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.transaction.annotation.EnableTransactionManagement
import spock.lang.Specification

import java.lang.reflect.Method
import java.lang.reflect.Modifier

class ApplicationSpec extends Specification {

    def "should be annotated with SpringBootApplication"() {
        when:
        def annotation = Application.getAnnotation(SpringBootApplication.class)

        then:
        annotation != null
    }

    def "should be annotated with EnableConfigurationProperties"() {
        when:
        def annotation = Application.getAnnotation(EnableConfigurationProperties.class)

        then:
        annotation != null
    }

    def "should be annotated with EnableTransactionManagement"() {
        when:
        def annotation = Application.getAnnotation(EnableTransactionManagement.class)

        then:
        annotation != null
    }

    def "should be annotated with EnableJpaRepositories"() {
        when:
        def annotation = Application.getAnnotation(EnableJpaRepositories.class)

        then:
        annotation != null
    }

    def "should be annotated with EnableAspectJAutoProxy"() {
        when:
        def annotation = Application.getAnnotation(EnableAspectJAutoProxy.class)

        then:
        annotation != null
    }

    def "should be annotated with EnableWebSecurity"() {
        when:
        def annotation = Application.getAnnotation(EnableWebSecurity.class)

        then:
        annotation != null
    }

    def "should have main method"() {
        when:
        Method mainMethod = Application.getMethod("main", String[].class)

        then:
        mainMethod != null
        Modifier.isStatic(mainMethod.modifiers)
        Modifier.isPublic(mainMethod.modifiers)
        mainMethod.returnType == void.class
    }

    def "should be in correct package"() {
        when:
        def packageName = Application.package.name

        then:
        packageName == "finance"
    }

    def "should be an open class for Spring Boot compatibility"() {
        when:
        def modifiers = Application.modifiers

        then:
        // In Kotlin, open classes are not final in Java reflection
        !Modifier.isFinal(modifiers)
        Modifier.isPublic(modifiers)
    }

    def "should have proper class structure"() {
        when:
        def applicationClass = Application

        then:
        applicationClass.name == "finance.Application"
        applicationClass.superclass == Object.class
        applicationClass.constructors.length >= 1  // Should have at least default constructor
    }

    def "should have companion object with main method"() {
        when:
        def companionClass = Application.getClasses().find { it.simpleName.contains("Companion") }

        then:
        companionClass != null
        // The main method should be accessible through the companion
        Application.getMethod("main", String[].class) != null
    }

    def "main method should be properly configured for Spring Boot"() {
        when:
        Method mainMethod = Application.getMethod("main", String[].class)

        then:
        mainMethod != null
        mainMethod.parameterTypes.length == 1
        mainMethod.parameterTypes[0] == String[].class
        mainMethod.returnType == void.class
        Modifier.isStatic(mainMethod.modifiers)
    }
}