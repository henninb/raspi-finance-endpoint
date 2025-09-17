package finance.services

import finance.helpers.AccountBuilder
import jakarta.persistence.EntityNotFoundException

class AccountServiceEdgeSpec extends BaseServiceSpec {

    AccountService service

    void setup() {
        service = new AccountService(accountRepositoryMock)
        service.validator = validatorMock
        service.meterService = meterService
    }

    void "deactivateAccount throws EntityNotFoundException when account missing"() {
        when:
        service.deactivateAccount('missing_acct')

        then:
        1 * accountRepositoryMock.findByAccountNameOwner('missing_acct') >> Optional.empty()
        thrown(EntityNotFoundException)
    }

    void "activateAccount throws EntityNotFoundException when account missing"() {
        when:
        service.activateAccount('missing_acct')

        then:
        1 * accountRepositoryMock.findByAccountNameOwner('missing_acct') >> Optional.empty()
        thrown(EntityNotFoundException)
    }
}

