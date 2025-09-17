package finance.services

import finance.configurations.DatabaseResilienceConfiguration
import jakarta.persistence.EntityNotFoundException
import org.springframework.dao.DataAccessResourceFailureException

class AccountServiceResilienceSpec extends BaseServiceSpec {

    AccountService service
    DatabaseResilienceConfiguration cfg

    void setup() {
        service = new AccountService(accountRepositoryMock)
        service.validator = validatorMock
        service.meterService = meterService

        // Wire real resilience components so BaseService takes the resilience path
        cfg = new DatabaseResilienceConfiguration()
        service.databaseResilienceConfig = cfg
        service.circuitBreaker = cfg.databaseCircuitBreaker()
        service.retry = cfg.databaseRetry()
        service.timeLimiter = cfg.databaseTimeLimiter()
        service.scheduledExecutorService = cfg.scheduledExecutorService()
    }

    void "deactivateAccount uses resilience path and unwraps EntityNotFoundException"() {
        when:
        service.deactivateAccount('nope')

        then:
        1 * accountRepositoryMock.findByAccountNameOwner('nope') >> Optional.empty()
        def ex = thrown(EntityNotFoundException)
        ex.message.contains('Account not found')
    }

    void "activateAccount uses resilience path and unwraps EntityNotFoundException"() {
        when:
        service.activateAccount('missing')

        then:
        1 * accountRepositoryMock.findByAccountNameOwner('missing') >> Optional.empty()
        def ex = thrown(EntityNotFoundException)
        ex.message.contains('Account not found')
    }

    void "deactivateAccount wraps non-entity exceptions as DataAccessResourceFailureException"() {
        given:
        def acct = finance.helpers.AccountBuilder.builder().withAccountNameOwner('err-deact').build()

        when:
        service.deactivateAccount('err-deact')

        then:
        1 * accountRepositoryMock.findByAccountNameOwner('err-deact') >> Optional.of(acct)
        1 * accountRepositoryMock.saveAndFlush(_ as finance.domain.Account) >> { throw new RuntimeException('boom') }
        thrown(DataAccessResourceFailureException)
    }

    void "activateAccount wraps non-entity exceptions as DataAccessResourceFailureException"() {
        given:
        def acct = finance.helpers.AccountBuilder.builder().withAccountNameOwner('err-activate').withActiveStatus(false).build()

        when:
        service.activateAccount('err-activate')

        then:
        1 * accountRepositoryMock.findByAccountNameOwner('err-activate') >> Optional.of(acct)
        1 * accountRepositoryMock.saveAndFlush(_ as finance.domain.Account) >> { throw new RuntimeException('boom2') }
        thrown(DataAccessResourceFailureException)
    }
}
