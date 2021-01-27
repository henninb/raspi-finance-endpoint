package finance.services

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import finance.domain.Account
import finance.helpers.AccountBuilder
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags

import javax.validation.ConstraintViolation
import javax.validation.ValidationException

class AccountServiceSpec extends BaseServiceSpec {
    protected AccountService accountService = new AccountService(accountRepositoryMock, validatorMock, meterService)

    protected String validJsonPayload  = '''
{
"accountNameOwner": "test_brian",
"accountType": "credit",
"activeStatus": "true",
"moniker": "0000",
"totals": 0.00,
"totalsBalanced": 0.00,
"dateClosed": 0
}
'''
    void 'test findAllActiveAccounts empty'() {
        given:
        Account account = AccountBuilder.builder().build()
        List<Account> accounts = []
        accounts.add(account)

        when:
        List<Account> results = accountService.findByActiveStatusOrderByAccountNameOwner()

        then:
        results.size() == 1
        1 * accountRepositoryMock.findByActiveStatusOrderByAccountNameOwner(true) >> accounts
        0 * _
    }

    void 'test findAllActiveAccounts'() {
        given:
        Account account = AccountBuilder.builder().build()
        List<Account> accounts = [account, account, account, account]

        when:
        List<Account> results = accountService.findByActiveStatusOrderByAccountNameOwner()

        then:
        results.size() == 4
        1 * accountRepositoryMock.findByActiveStatusOrderByAccountNameOwner(true) >> accounts
        0 * _
    }

    void 'test insertAccount - attempt to insert a preexisting account'() {
        given:
        Account account = AccountBuilder.builder().build()
        Set<ConstraintViolation<Account>> constraintViolations = validator.validate(account)

        when:
        Boolean isInserted = accountService.insertAccount(account)

        then:
        !isInserted
        constraintViolations.size() == 0
        1 * validatorMock.validate(account) >> constraintViolations
        1 * accountRepositoryMock.findByAccountNameOwner(account.accountNameOwner) >> Optional.of(account)
        0 * _
    }

    void 'test insertAccount - attempt to insert a empty accountNameOwner'() {
        given:
        Account account = AccountBuilder.builder().withAccountNameOwner('').build()
        Set<ConstraintViolation<Account>> constraintViolations = validator.validate(account)
        Tags tags = Tags.of(validationExceptionTag, serverNameTag)
        Meter.Id id = new Meter.Id("exception.caught.counter", tags, null, null, Meter.Type.COUNTER)

        when:
        accountService.insertAccount(account)

        then:
        thrown(ValidationException)
        constraintViolations.size() == 2
        1 * validatorMock.validate(account) >> constraintViolations
        1 * accountRepositoryMock.findByAccountNameOwner(account.accountNameOwner) >> Optional.of(account)
        1 * meterRegistryMock.counter(id) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'test insertAccount - json inserted success'() {
        given:
        Account account = mapper.readValue(validJsonPayload, Account)
        Set<ConstraintViolation<Account>> constraintViolations = validator.validate(account)

        when:
        Boolean isInserted = accountService.insertAccount(account)

        then:
        isInserted
        1 * validatorMock.validate(account) >> constraintViolations
        1 * accountRepositoryMock.findByAccountNameOwner(account.accountNameOwner) >> Optional.empty()
        1 * accountRepositoryMock.saveAndFlush(account)
        0 * _
    }

    void 'test insertAccount - invalid moniker'() {
        given:
        Account account = AccountBuilder.builder().withMoniker('12345').build()
        Set<ConstraintViolation<Account>> constraintViolations = validator.validate(account)

        when:
        accountService.insertAccount(account)

        then:
        constraintViolations.size() == 1
        ValidationException ex = thrown(ValidationException)
        ex.message.contains('Cannot insert account as there is a constraint violation')
        1 * accountRepositoryMock.findByAccountNameOwner(account.accountNameOwner) >> Optional.of(account)
        1 * validatorMock.validate(account) >> constraintViolations
        1 * meterRegistryMock.counter(_) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'test insertAccount - bad json - accountType'() {
        given:
        String jsonPayload = "{\"accountId\":1001,\"accountNameOwner\":\"discover_brian\",\"accountType\":\"Credit\",\"activeStatus\":true,\"moniker\":\"1234\",\"totals\":0.01,\"totalsBalanced\":0.02,\"dateClosed\":0}"

        when:
        mapper.readValue(jsonPayload, Account)

        then:
        InvalidFormatException ex = thrown()
        ex.message.contains('not one of the values accepted for Enum class')
        0 * _
    }
}
