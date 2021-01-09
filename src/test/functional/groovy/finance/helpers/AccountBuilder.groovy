package finance.helpers

import finance.domain.Account
import finance.domain.AccountType

import java.sql.Timestamp

class AccountBuilder {

    String accountNameOwner = 'foo_brian'
    AccountType accountType = AccountType.Credit
    Boolean activeStatus = true
    String moniker = '0000'
    BigDecimal totals = new BigDecimal(0)
    BigDecimal totalsBalanced = new BigDecimal(0)
    Timestamp dateClosed = new Timestamp(0)

    static AccountBuilder builder() {
        return new AccountBuilder()
    }

    Account build() {
        Account account = new Account().with {
            accountNameOwner = this.accountNameOwner
            accountType = this.accountType
            activeStatus = this.activeStatus
            moniker = this.moniker
            totals = this.totals
            totalsBalanced = this.totalsBalanced
            dateClosed = this.dateClosed
            return it
        }
        return account
    }

    AccountBuilder withAccountNameOwner(String accountNameOwner) {
        this.accountNameOwner = accountNameOwner
        return this
    }

    AccountBuilder withAccountType(AccountType accountType) {
        this.accountType = accountType
        return this
    }

    AccountBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        return this
    }

    AccountBuilder withMoniker(String moniker) {
        this.moniker = moniker
        return this
    }

    AccountBuilder withDateClosed(Timestamp dateClosed) {
        this.dateClosed = dateClosed
        return this
    }

    AccountBuilder withTotals(BigDecimal totals) {
        this.totals = totals
        return this
    }

    AccountBuilder withTotalsBalanced(BigDecimal totalsBalanced) {
        this.totalsBalanced = totalsBalanced
        return this
    }
}
