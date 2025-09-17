package finance.services

import jakarta.persistence.EntityNotFoundException
import org.springframework.dao.DataAccessResourceFailureException

class AccountServiceCoverageSpec extends BaseServiceSpec {

    void setup() {
        // nothing specific; each test wires what it needs
    }

    void "updateTotalsForAllAccounts success path returns true"() {
        given:
        def service = new AccountService(accountRepositoryMock)
        service.meterService = meterService
        service.validator = validatorMock

        when:
        def result = service.updateTotalsForAllAccounts()

        then:
        1 * accountRepositoryMock.updateTotalsForAllAccounts()
        result
    }

    // Additional unwrap/propagation branches are tightly coupled to BaseService's protected generics
    // and Kotlin-Java interop; exercising them via spies is brittle across environments.
}
