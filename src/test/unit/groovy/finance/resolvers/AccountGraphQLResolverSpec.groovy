package finance.resolvers

import finance.domain.Account
import finance.domain.AccountType
import finance.services.BaseServiceSpec
import graphql.schema.DataFetchingEnvironment

import java.sql.Timestamp

class AccountGraphQLResolverSpec extends BaseServiceSpec {
    def resolver = new AccountGraphQLResolver(accountService)

    void setup() {
        accountService.meterService = meterService
        accountService.validator = validator
    }

    private static Account makeAccount(Long id = 1L, String owner = 'checking_primary') {
        new Account(
                id,
                owner,
                AccountType.Checking,
                true,
                '1234',
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new Timestamp(0),
                new Timestamp(System.currentTimeMillis())
        )
    }

    def "accounts fetcher returns list from service"() {
        given:
        def env = Mock(DataFetchingEnvironment)
        def accounts = [makeAccount(1L, 'checking_primary'), makeAccount(2L, 'savings_primary')]
        accountRepositoryMock.findByActiveStatusOrderByAccountNameOwner(true) >> accounts

        when:
        def result = resolver.accounts.get(env)

        then:
        result == accounts
    }

    def "account fetcher returns single account when present"() {
        given:
        def env = Mock(DataFetchingEnvironment) {
            getArgument('accountNameOwner') >> 'checking_primary'
        }
        def acct = makeAccount(42L, 'checking_primary')
        accountRepositoryMock.findByAccountNameOwner('checking_primary') >> Optional.of(acct)

        when:
        def result = resolver.account().get(env)

        then:
        result == acct
    }

    def "account fetcher returns null when optional empty"() {
        given:
        def env = Mock(DataFetchingEnvironment) {
            getArgument('accountNameOwner') >> 'nonexistent'
        }
        accountRepositoryMock.findByAccountNameOwner('nonexistent') >> Optional.empty()

        expect:
        resolver.account().get(env) == null
    }

    def "account fetcher throws when argument missing"() {
        given:
        def env = Mock(DataFetchingEnvironment) {
            getArgument('accountNameOwner') >> null
        }

        when:
        resolver.account().get(env)

        then:
        thrown(IllegalArgumentException)
    }
}

