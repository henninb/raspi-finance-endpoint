package finance.configurations

import spock.lang.Specification

class NoOpResilienceConfigurationSpec extends Specification {

    def "resilienceComponents bean returns non-null instance"() {
        given:
        def config = new NoOpResilienceConfiguration()

        when:
        def components = config.resilienceComponents()

        then:
        components != null
        components.circuitBreaker != null
        components.retry != null
        components.timeLimiter != null
        components.scheduledExecutorService != null
    }

    def "resilienceComponents bean returns the noOp singleton"() {
        given:
        def config = new NoOpResilienceConfiguration()

        when:
        def c1 = config.resilienceComponents()
        def c2 = config.resilienceComponents()

        then:
        c1.is(c2)
    }

    def "resilienceComponents databaseResilienceConfig is NoOpDatabaseResilienceConfiguration"() {
        given:
        def config = new NoOpResilienceConfiguration()

        when:
        def components = config.resilienceComponents()

        then:
        components.databaseResilienceConfig instanceof NoOpDatabaseResilienceConfiguration
    }
}
