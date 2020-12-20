package finance.helpers

import finance.domain.Account
import finance.domain.AccountType

import java.sql.Timestamp

class AccountBuilder {

    String accountNameOwner = 'foo_brian'
    AccountType accountType = AccountType.Credit
    Boolean activeStatus = true
    String moniker = '0000'
    BigDecimal totals = 0.00G
    BigDecimal totalsBalanced = 0.00G
    Timestamp dateClosed = new Timestamp(0)

    static AccountBuilder builder() {
        return new AccountBuilder()
    }

    Account build() {
        Account account = new Account()
        account.accountNameOwner = accountNameOwner
        account.accountType = accountType
        account.activeStatus = activeStatus
        account.moniker = moniker
        account.totals = totals
        account.totalsBalanced = totalsBalanced
        account.dateClosed = dateClosed
        return account
    }

    AccountBuilder accountNameOwner(String accountNameOwner) {
        this.accountNameOwner = accountNameOwner
        return this
    }

    AccountBuilder accountType(AccountType accountType) {
        this.accountType = accountType
        return this
    }

    AccountBuilder activeStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        return this
    }

    AccountBuilder moniker(String moniker) {
        this.moniker = moniker
        return this
    }

    AccountBuilder dateClosed(Timestamp dateClosed) {
        this.dateClosed = dateClosed
        return this
    }

    AccountBuilder totals(BigDecimal totals) {
        this.totals = totals
        return this
    }

    AccountBuilder totalsBalanced(BigDecimal totalsBalanced) {
        this.totalsBalanced = totalsBalanced
        return this
    }
}
