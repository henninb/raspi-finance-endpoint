package finance.helpers

import finance.domain.User

class UserBuilder {

    String username = 'test_user'
    String password = 'test_password'
    boolean activeStatus = true
    String firstName = 'test'
    String lastName = 'user'

    static UserBuilder builder() {
        return new UserBuilder()
    }

    User build() {
        User user = new User().with {
            userId = 0L
            username = this.username
            password = this.password
            activeStatus = this.activeStatus
            firstName = this.firstName
            lastName = this.lastName
            return it
        }
        return user
    }

    UserBuilder withUsername(String username) {
        this.username = username
        return this
    }

    UserBuilder withPassword(String password) {
        this.password = password
        return this
    }

    UserBuilder withActiveStatus(boolean activeStatus) {
        this.activeStatus = activeStatus
        return this
    }
    
    UserBuilder withFirstName(String firstName) {
        this.firstName = firstName
        return this
    }
    
    UserBuilder withLastName(String lastName) {
        this.lastName = lastName
        return this
    }
}
