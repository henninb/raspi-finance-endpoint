package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Account
import finance.repositories.AccountRepository
import spock.lang.Ignore
import spock.lang.Specification

import javax.validation.Validator

class AccountServiceSpec extends Specification {
    AccountRepository mockAccountRepository = Mock(AccountRepository)
    Validator mockValidator = Mock(Validator)
    AccountService accountService = new AccountService(mockAccountRepository,mockValidator)
    private ObjectMapper mapper = new ObjectMapper()

    def "test findAllActiveAccounts empty"() {
        given:
        List<Account> accounts = []

        when:
        accountService.findAllActiveAccounts()

        then:
        1 * mockAccountRepository.findByActiveStatusOrderByAccountNameOwner(true) >> accounts
        0 * _
    }

    def "test findAllActiveAccounts"() {
        given:
        List<Account> accounts = []
        Account account = {
        } as Account
        accounts.add(account)

        when:
        accountService.findAllActiveAccounts()

        then:
        1 * mockAccountRepository.findByActiveStatusOrderByAccountNameOwner(true) >> accounts
        0 * _
    }

    def "test insertAccount"() {
        given:
        def jsonPayload = "{\"accountId\":1001,\"accountNameOwner\":\"discover_brian\",\"accountType\":\"credit\",\"activeStatus\":true,\"moniker\":\"1234\",\"totals\":0.01,\"totalsBalanced\":0.02,\"dateClosed\":0,\"dateUpdated\":1553645394000,\"dateAdded\":1553645394000}"
        Account account = mapper.readValue(jsonPayload, Account.class)

        when:
        def isInserted = accountService.insertAccount(account)

        then:
        isInserted
        1 * mockValidator.validate(account) >> new HashSet()
        1 * mockAccountRepository.saveAndFlush(account)
        0 * _
    }

    def "test insertAccount - invalid moniker"() {
        given:
        def jsonPayload = "{\"accountId\":1001,\"accountNameOwner\":\"discover_brian\",\"accountType\":\"credit\",\"activeStatus\":true,\"moniker\":\"12345\",\"totals\":0.0112,\"totalsBalanced\":0.02,\"dateClosed\":0,\"dateUpdated\":1553645394000,\"dateAdded\":1553645394000}"
        Account account = mapper.readValue(jsonPayload, Account.class)

        when:
        def isInserted = accountService.insertAccount(account)

        then:
        isInserted
        1 * mockValidator.validate(account) >> new HashSet()
        1 * mockAccountRepository.saveAndFlush(account)
        0 * _
    }

    @Ignore
    //InvalidFormatException
    def "test insertAccount - bad accountType"() {
        given:
        def jsonPayload = "{\"accountId\":1001,\"accountNameOwner\":\"discover_brian\",\"accountType\":\"badAccountType\",\"activeStatus\":true,\"moniker\":\"1234\",\"totals\":0.01,\"totalsBalanced\":0.02,\"dateClosed\":0,\"dateUpdated\":1553645394000,\"dateAdded\":1553645394000}"
        Account account = mapper.readValue(jsonPayload, Account.class)

        when:
        def isInserted = accountService.insertAccount(account)

        then:
        isInserted
        1 * mockValidator.validate(account) >> new HashSet()
        1 * mockAccountRepository.saveAndFlush(account)
        0 * _
    }
}
