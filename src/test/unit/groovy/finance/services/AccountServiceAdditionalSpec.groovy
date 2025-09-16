package finance.services

import finance.domain.Account
import finance.domain.AccountType
import finance.repositories.AccountRepository
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.dao.InvalidDataAccessResourceUsageException

class AccountServiceAdditionalSpec extends BaseServiceSpec {

    void 'updateTotalsForAllAccounts - catches InvalidDataAccessResourceUsageException and increments metric'() {
        given:
        // real meter to verify counter
        def registry = new SimpleMeterRegistry()
        def service = new AccountService(accountRepositoryMock)
        service.meterService = new MeterService(registry)
        service.validator = validatorMock

        when:
        def result = service.updateTotalsForAllAccounts()

        then:
        1 * accountRepositoryMock.updateTotalsForAllAccounts() >> { throw new InvalidDataAccessResourceUsageException('bad') }
        result
        def c = registry.find('exception.caught.counter').tag('exception.name.tag','InvalidDataAccessResourceUsageException').counter()
        assert c != null && c.count() >= 1
    }

    void 'findAccountsThatRequirePayment calls updateTotals then queries repository'() {
        given:
        def service = GroovySpy(AccountService, constructorArgs: [accountRepositoryMock])
        service.validator = validatorMock
        service.meterService = new MeterService(new SimpleMeterRegistry())

        def a = new Account(accountId: 1L, accountNameOwner: 'acct', accountType: AccountType.Credit)

        when:
        def result = service.findAccountsThatRequirePayment()

        then:
        1 * service.updateTotalsForAllAccounts() >> true
        1 * accountRepositoryMock.findAccountsThatRequirePayment(true, AccountType.Credit) >> [a]
        result.size() == 1
        result[0].accountId == 1L
    }
}
