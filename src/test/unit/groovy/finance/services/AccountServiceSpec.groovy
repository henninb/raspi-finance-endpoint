package finance.services

import spock.lang.Specification

class AccountServiceSpec extends Specification {
    void setup() {
        def y = 3
    }

    def "my test"() {
        given:
         def x  = 1
        when:
         x = x + 1
        then:
        1 == 1
    }
}
