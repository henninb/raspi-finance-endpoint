package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import finance.domain.Account
import finance.helpers.AccountBuilder
import finance.repositories.AccountRepository
import spock.lang.Specification

import javax.validation.ConstraintViolation
import javax.validation.Validation
import javax.validation.Validator

class AccountServiceSpec extends Specification {
    AccountRepository mockAccountRepository = Mock(AccountRepository)
    Validator mockValidator = Mock(Validator)
    AccountService accountService = new AccountService(mockAccountRepository, mockValidator)
    private ObjectMapper mapper = new ObjectMapper()

    def "test findAllActiveAccounts empty"() {
        given:
        Account account = AccountBuilder.builder().build()
        List<Account> accounts = []
        accounts.add(account)

        when:
        def results = accountService.findByActiveStatusOrderByAccountNameOwner()

        then:
        results.size() == 1
        1 * mockAccountRepository.findByActiveStatusOrderByAccountNameOwner(true) >> accounts
        0 * _
    }

    def "test findAllActiveAccounts"() {
        given:
        Account account = AccountBuilder.builder().build()
        List<Account> accounts = []
        accounts.add(account)

        when:
        def results = accountService.findByActiveStatusOrderByAccountNameOwner()

        then:
        results.size() == 1
        1 * mockAccountRepository.findByActiveStatusOrderByAccountNameOwner(true) >> accounts
        0 * _
    }

    def "test insertAccount - existing"() {
        given:
        def jsonPayload = "{\"accountNameOwner\":\"discover_brian\",\"accountType\":\"credit\",\"activeStatus\":true,\"moniker\":\"1234\",\"totals\":0.01,\"totalsBalanced\":0.02,\"dateClosed\":0,\"dateUpdated\":1553645394000,\"dateAdded\":1553645394000}"
        Account account = mapper.readValue(jsonPayload, Account.class)

        when:
        def isInserted = accountService.insertAccount(account)

        then:
        isInserted.is(true)
        1 * mockValidator.validate(account) >> new HashSet()
        1 * mockAccountRepository.findByAccountNameOwner(account.accountNameOwner) >> Optional.of(account)
        0 * _
    }

    def "test insertAccount - json inserted success"() {
        given:
        def jsonPayload = "{\"accountNameOwner\":\"discover_brian\",\"accountType\":\"credit\",\"activeStatus\":true,\"moniker\":\"1234\",\"totals\":0.01,\"totalsBalanced\":0.02,\"dateClosed\":0,\"dateUpdated\":1553645394000,\"dateAdded\":1553645394000}"
        Account account = mapper.readValue(jsonPayload, Account.class)

        when:
        def isInserted = accountService.insertAccount(account)

        then:
        isInserted.is(true)
        1 * mockValidator.validate(account) >> new HashSet()
        1 * mockAccountRepository.findByAccountNameOwner(account.accountNameOwner) >> Optional.empty()
        1 * mockAccountRepository.saveAndFlush(account)
        0 * _
    }

    def "test insertAccount - invalid moniker"() {
        given:
        def jsonPayload = "{\"accountId\":1001,\"accountNameOwner\":\"discover_brian\",\"accountType\":\"credit\",\"activeStatus\":true,\"moniker\":\"12345\",\"totals\":0.0112,\"totalsBalanced\":0.02,\"dateClosed\":0,\"dateUpdated\":1553645394000,\"dateAdded\":1553645394000}"
        Account account = mapper.readValue(jsonPayload, Account.class)
        def validatorFactory = Validation.buildDefaultValidatorFactory()
        def validator = validatorFactory.getValidator()
        Set<ConstraintViolation<Account>> constraintViolations = validator.validate(account)

        when:
        accountService.insertAccount(account)

        then:
        1 * mockAccountRepository.findByAccountNameOwner(account.accountNameOwner) >> Optional.of(account)
        1 * mockValidator.validate(account) >> constraintViolations
        0 * _
    }

    def "test insertAccount - invalid dateAdded"() {
        given:
        def jsonPayload = "{\"accountId\":1001,\"accountNameOwner\":\"discover_brian\",\"accountType\":\"credit\",\"activeStatus\":true,\"moniker\":\"1234\",\"totals\":0.0112,\"totalsBalanced\":0.02,\"dateClosed\":0,\"dateUpdated\":1553645394000,\"dateAdded\":1553645394}"
        Account account = mapper.readValue(jsonPayload, Account.class)
        def validatorFactory = Validation.buildDefaultValidatorFactory()
        def validator = validatorFactory.getValidator()
        Set<ConstraintViolation<Account>> constraintViolations = validator.validate(account)

        when:
        accountService.insertAccount(account)

        then:
        1 * mockAccountRepository.findByAccountNameOwner(account.accountNameOwner) >> Optional.of(account)
        1 * mockValidator.validate(account) >> constraintViolations
        0 * _
    }


    def "test insertAccount - bad json - accountType"() {
        given:
        def jsonPayload = "{\"accountId\":1001,\"accountNameOwner\":\"discover_brian\",\"accountType\":\"Credit\",\"activeStatus\":true,\"moniker\":\"1234\",\"totals\":0.01,\"totalsBalanced\":0.02,\"dateClosed\":0,\"dateUpdated\":1553645394000,\"dateAdded\":1553645394000}"

        when:
        mapper.readValue(jsonPayload, Account.class)

        then:
        thrown InvalidFormatException
    }
}
