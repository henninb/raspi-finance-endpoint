package finance.services

import finance.domain.Account
import finance.repositories.AccountRepository
import spock.lang.Specification

class AccountServiceSpec extends Specification {
    AccountRepository mockAccountRepository = Mock(AccountRepository)
    AccountService accountService = new AccountService(mockAccountRepository)

    def "test findAllActiveAccounts empty"() {
        given:
        List<Account> accounts = []

        when:
        accountService.findAllActiveAccounts()

        then:
        1 * mockAccountRepository.findByActiveStatusOrderByAccountNameOwner(true) >> accounts
        1 == 1
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
        1 == 1
    }

}
