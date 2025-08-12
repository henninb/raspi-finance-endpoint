package finance.helpers

import finance.domain.Account
import finance.domain.AccountType

import java.sql.Timestamp

class AccountBuilder {

    String accountNameOwner = 'foo_brian'
    AccountType accountType = AccountType.Credit
    Boolean activeStatus = true
    String moniker = '0000'
    BigDecimal future = new BigDecimal(0)
    BigDecimal outstanding = new BigDecimal(0)
    BigDecimal cleared = new BigDecimal(0)
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
            future = this.future
            outstanding = this.outstanding
            cleared = this.cleared
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

    AccountBuilder withFuture(BigDecimal future) {
        this.future = future
        return this
    }

    AccountBuilder withOutstanding(BigDecimal outstanding) {
        this.outstanding = outstanding
        return this
    }

    AccountBuilder withCleared(BigDecimal cleared) {
        this.cleared = cleared
        return this
    }

}
