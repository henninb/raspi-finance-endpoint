package finance.configurations

import spock.lang.Specification

class DataSourcePropertiesSpec extends Specification {

    def "defaults are empty strings"() {
        when:
        def props = new DataSourceProperties()

        then:
        props.url == ''
        props.username == ''
        props.password == ''
    }

    def "setters update values"() {
        given:
        def props = new DataSourceProperties()

        when:
        props.url = 'jdbc:postgresql://localhost:5432/db'
        props.username = 'user'
        props.password = 'secret'

        then:
        props.url.contains('jdbc:postgresql')
        props.username == 'user'
        props.password == 'secret'
    }
}

