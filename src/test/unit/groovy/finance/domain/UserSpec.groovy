package finance.domain

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification

import java.sql.Timestamp
import java.util.*

class UserSpec extends Specification {

    def "User - default constructor"() {
        when:
        def user = new User()

        then:
        user.userId == 0L
        user.activeStatus == true
        user.firstName == ""
        user.lastName == ""
        user.username == ""
        user.password == ""
        user.dateAdded instanceof Timestamp
        user.dateUpdated instanceof Timestamp
    }

    def "User - parameterized constructor"() {
        given:
        def userId = 1L
        def activeStatus = false
        def firstName = "john"
        def lastName = "doe"
        def username = "johndoe"
        def password = "password123"

        when:
        def user = new User(userId, activeStatus, firstName, lastName, username, password)

        then:
        user.userId == userId
        user.activeStatus == activeStatus
        user.firstName == firstName
        user.lastName == lastName
        user.username == username
        user.password == password
        user.dateAdded instanceof Timestamp
        user.dateUpdated instanceof Timestamp
    }

    def "User - toString returns valid JSON"() {
        given:
        def user = new User(1L, true, "john", "doe", "johndoe", "password123")

        when:
        def json = user.toString()
        def mapper = new ObjectMapper()
        def parsedUser = mapper.readValue(json, Map.class)

        then:
        json != null
        parsedUser.userId == 1
        parsedUser.activeStatus == true
        parsedUser.firstName == "john"
        parsedUser.lastName == "doe"
        parsedUser.username == "johndoe"
        parsedUser.password == null  // password is @get:JsonIgnore, excluded from serialization
    }

    def "User - properties can be modified"() {
        given:
        def user = new User()

        when:
        user.userId = 5L
        user.activeStatus = false
        user.firstName = "jane"
        user.lastName = "smith"
        user.username = "janesmith"
        user.password = "newpassword"

        then:
        user.userId == 5L
        user.activeStatus == false
        user.firstName == "jane"
        user.lastName == "smith"
        user.username == "janesmith"
        user.password == "newpassword"
    }

    def "User - timestamps are automatically set"() {
        given:
        def beforeCreation = System.currentTimeMillis()

        when:
        def user = new User()
        def afterCreation = System.currentTimeMillis()

        then:
        user.dateAdded.time >= beforeCreation
        user.dateAdded.time <= afterCreation
        user.dateUpdated.time >= beforeCreation
        user.dateUpdated.time <= afterCreation
    }
}