package finance.helpers

import finance.domain.Account
import finance.domain.AccountType

import java.sql.Timestamp

class AccountBuilder {

    Long accountId = 0L
    String accountNameOwner = 'foo_brian'
    AccountType accountType = AccountType.Credit
    Boolean activeStatus = true
    String moniker = '0000'
    BigDecimal future = new BigDecimal(0)
    BigDecimal outstanding = new BigDecimal(0)
    BigDecimal cleared = new BigDecimal(0)
    Timestamp dateClosed = new Timestamp(0)
    Timestamp dateAdded = new Timestamp(System.currentTimeMillis())
    Timestamp dateUpdated = new Timestamp(System.currentTimeMillis())

    static AccountBuilder builder() {
        return new AccountBuilder()
    }

    Account build() {
        Account account = new Account().with {
            accountId = this.accountId
            accountNameOwner = this.accountNameOwner
            accountType = this.accountType
            activeStatus = this.activeStatus
            moniker = this.moniker
            future = this.future
            outstanding = this.outstanding
            cleared = this.cleared
            dateClosed = this.dateClosed
            dateAdded = this.dateAdded
            dateUpdated = this.dateUpdated
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

    AccountBuilder withAccountId(Long accountId) {
        this.accountId = accountId
        return this
    }

    AccountBuilder withDateAdded(Timestamp dateAdded) {
        this.dateAdded = dateAdded
        return this
    }

    AccountBuilder withDateUpdated(Timestamp dateUpdated) {
        this.dateUpdated = dateUpdated
        return this
    }

}
