package finance.configurations

import spock.lang.Specification

class CustomPropertiesSpec extends Specification {

    def "should create CustomProperties with default empty excluded accounts list"() {
        when:
        CustomProperties properties = new CustomProperties()

        then:
        properties.excludedAccounts != null
        properties.excludedAccounts.isEmpty()
        properties.excludedAccounts instanceof List
    }

    def "should accept excluded accounts in constructor"() {
        given:
        def excludedAccounts = ["account1", "account2"]

        when:
        CustomProperties properties = new CustomProperties(excludedAccounts)

        then:
        properties.excludedAccounts == excludedAccounts
        properties.excludedAccounts.size() == 2
        properties.excludedAccounts.contains("account1")
        properties.excludedAccounts.contains("account2")
    }

    def "should allow modification of excluded accounts list"() {
        given:
        CustomProperties properties = new CustomProperties()

        when:
        properties.excludedAccounts.add("test_account")
        properties.excludedAccounts.add("another_account")

        then:
        properties.excludedAccounts.size() == 2
        properties.excludedAccounts.contains("test_account")
        properties.excludedAccounts.contains("another_account")
    }

    def "should allow setting new excluded accounts list"() {
        given:
        CustomProperties properties = new CustomProperties()
        def newList = ["new_account1", "new_account2"]

        when:
        properties.excludedAccounts = newList

        then:
        properties.excludedAccounts == newList
        properties.excludedAccounts.size() == 2
    }
}