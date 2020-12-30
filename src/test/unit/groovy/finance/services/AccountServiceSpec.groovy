package finance.services


import com.fasterxml.jackson.databind.exc.InvalidFormatException
import finance.domain.Account
import finance.helpers.AccountBuilder
import javax.validation.*

class AccountServiceSpec extends BaseServiceSpec {
    protected AccountService accountService = new AccountService(accountRepositoryMock, validatorMock, meterServiceMock)

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
        List<Account> accounts = []
        accounts.add(account)

        when:
        List<Account> results = accountService.findByActiveStatusOrderByAccountNameOwner()

        then:
        results.size() == 1
        1 * accountRepositoryMock.findByActiveStatusOrderByAccountNameOwner(true) >> accounts
        0 * _
    }

    void 'test insertAccount - existing'() {
        given:
        String jsonPayload = "{\"accountNameOwner\":\"discover_brian\",\"accountType\":\"credit\",\"activeStatus\":true,\"moniker\":\"1234\",\"totals\":0.01,\"totalsBalanced\":0.02,\"dateClosed\":0,\"dateUpdated\":1553645394000,\"dateAdded\":1553645394000}"
        Account account = mapper.readValue(jsonPayload, Account)

        when:
        Boolean isInserted = accountService.insertAccount(account)

        then:
        isInserted.is(false)
        1 * validatorMock.validate(account) >> ([] as Set)
        1 * accountRepositoryMock.findByAccountNameOwner(account.accountNameOwner) >> Optional.of(account)
        0 * _
    }

    void 'test insertAccount - json inserted success'() {
        given:
        String jsonPayload = "{\"accountNameOwner\":\"discover_brian\",\"accountType\":\"credit\",\"activeStatus\":true,\"moniker\":\"1234\",\"totals\":0.01,\"totalsBalanced\":0.02,\"dateClosed\":0,\"dateUpdated\":1553645394000,\"dateAdded\":1553645394000}"
        Account account = mapper.readValue(jsonPayload, Account)

        when:
        Boolean isInserted = accountService.insertAccount(account)

        then:
        isInserted.is(true)
        1 * validatorMock.validate(account) >> ([] as Set)
        1 * accountRepositoryMock.findByAccountNameOwner(account.accountNameOwner) >> Optional.empty()
        1 * accountRepositoryMock.saveAndFlush(account)
        0 * _
    }

    void 'test insertAccount - invalid moniker'() {
        given:
        String jsonPayload = "{\"accountId\":1001,\"accountNameOwner\":\"discover_brian\",\"accountType\":\"credit\",\"activeStatus\":true,\"moniker\":\"12345\",\"totals\":0.0112,\"totalsBalanced\":0.02,\"dateClosed\":0,\"dateUpdated\":1553645394000,\"dateAdded\":1553645394000}"
        Account account = mapper.readValue(jsonPayload, Account)
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()
        Validator validator = validatorFactory.validator
        Set<ConstraintViolation<Account>> constraintViolations = validator.validate(account)

        when:
        accountService.insertAccount(account)

        then:
        ValidationException ex = thrown(ValidationException)
        ex.message.contains('Cannot insert account as there is a constraint violation')
        1 * accountRepositoryMock.findByAccountNameOwner(account.accountNameOwner) >> Optional.of(account)
        1 * validatorMock.validate(account) >> constraintViolations
        0 * _
    }

    void 'test insertAccount - invalid dateAdded'() {
        given:
        String jsonPayload = "{\"accountId\":1001,\"accountNameOwner\":\"discover_brian\",\"accountType\":\"credit\",\"activeStatus\":true,\"moniker\":\"1234\",\"totals\":0.0112,\"totalsBalanced\":0.02,\"dateClosed\":0,\"dateUpdated\":1553645394000,\"dateAdded\":1553645394}"
        Account account = mapper.readValue(jsonPayload, Account)
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()
        Validator validator = validatorFactory.validator
        Set<ConstraintViolation<Account>> constraintViolations = validator.validate(account)

        when:
        accountService.insertAccount(account)

        then:
        ValidationException ex = thrown(ValidationException)
        ex.message.contains('Cannot insert account as there is a constraint violation')
        1 * accountRepositoryMock.findByAccountNameOwner(account.accountNameOwner) >> Optional.of(account)
        1 * validatorMock.validate(account) >> constraintViolations
        0 * _
    }

    void 'test insertAccount - bad json - accountType'() {
        given:
        String jsonPayload = "{\"accountId\":1001,\"accountNameOwner\":\"discover_brian\",\"accountType\":\"Credit\",\"activeStatus\":true,\"moniker\":\"1234\",\"totals\":0.01,\"totalsBalanced\":0.02,\"dateClosed\":0,\"dateUpdated\":1553645394000,\"dateAdded\":1553645394000}"

        when:
        mapper.readValue(jsonPayload, Account)

        then:
        InvalidFormatException ex = thrown()
        ex.message.contains('not one of the values accepted for Enum class')
        0 * _
    }
}
