package finance.services

import finance.utils.Constants
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import spock.lang.Ignore
import spock.lang.Specification

class MeterServiceSpec extends Specification {

    protected MeterRegistry meterRegistryMock = GroovyMock(MeterRegistry)
    protected MeterService meterService = new MeterService(meterRegistryMock)

    @Ignore
    void 'test increment account list is empty' () {
        when:
        meterService.incrementAccountListIsEmpty('test')

        then:
        //1 * meterRegistryMock.counter(Constants.TRANSACTION_ACCOUNT_LIST_NONE_FOUND_COUNTER, [Tag.of(Constants.ACCOUNT_NAME_OWNER_TAG, 'test'), Tag.of(Constants.SERVER_NAME_TAG, 'server')])
        1 * meterRegistryMock.counter(_, _)
        noExceptionThrown()
        0 * _
    }
}