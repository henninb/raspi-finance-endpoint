package finance.services

import finance.domain.Account
import finance.domain.AccountValidationException
import finance.helpers.AccountBuilder
import finance.helpers.TransactionBuilder
import io.micrometer.core.instrument.simple.SimpleMeterRegistry

class TransactionServiceChangeOwnerSpec extends BaseServiceSpec {

    void setup() {
        transactionService.accountService = accountServiceMock
        transactionService.transactionRepository = transactionRepositoryMock
        transactionService.meterService = new MeterService(new SimpleMeterRegistry())
    }

    void 'changeAccountNameOwner - account not found throws AccountValidationException'() {
        given:
        def guid = 'g'
        def tx = TransactionBuilder.builder().withGuid(guid).build()
        def map = [guid: guid, accountNameOwner: 'missing']

        when:
        transactionService.changeAccountNameOwner(map)

        then:
        1 * transactionRepositoryMock.findByGuid(guid) >> Optional.of(tx)
        1 * accountServiceMock.account('missing') >> Optional.empty()
        thrown(AccountValidationException)
    }

    void 'changeAccountNameOwner - null inputs increment metric and throw'() {
        given:
        def registry = new SimpleMeterRegistry()
        transactionService.meterService = new MeterService(registry)

        when:
        transactionService.changeAccountNameOwner([guid: null, accountNameOwner: null])

        then:
        thrown(AccountValidationException)
        def c = registry.find('exception.thrown.counter').tag('exception.name.tag','AccountValidationException').counter()
        assert c != null && c.count() >= 1
    }
}
