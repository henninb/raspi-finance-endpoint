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
        CustomProperties properties = new CustomProperties(excludedAccounts, [])

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

    def "should create CustomProperties with default empty adminUsers list"() {
        when:
        CustomProperties properties = new CustomProperties()

        then:
        properties.adminUsers != null
        properties.adminUsers.isEmpty()
        properties.adminUsers instanceof List
    }

    def "should accept adminUsers in constructor"() {
        given:
        def adminUsers = ["admin1", "admin2"]

        when:
        CustomProperties properties = new CustomProperties([], adminUsers)

        then:
        properties.adminUsers == adminUsers
        properties.adminUsers.size() == 2
        properties.adminUsers.contains("admin1")
        properties.adminUsers.contains("admin2")
    }

    def "should allow modification of adminUsers list"() {
        given:
        CustomProperties properties = new CustomProperties()

        when:
        properties.adminUsers.add("superuser")

        then:
        properties.adminUsers.size() == 1
        properties.adminUsers.contains("superuser")
    }

    def "should allow setting new adminUsers list"() {
        given:
        CustomProperties properties = new CustomProperties()

        when:
        properties.adminUsers = ["root", "admin"]

        then:
        properties.adminUsers == ["root", "admin"]
    }

    def "both lists are independent"() {
        given:
        CustomProperties properties = new CustomProperties(["excluded"], ["admin"])

        expect:
        properties.excludedAccounts == ["excluded"]
        properties.adminUsers == ["admin"]
    }
}