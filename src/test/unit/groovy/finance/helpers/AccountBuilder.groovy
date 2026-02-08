package finance.helpers

import finance.domain.Account
import finance.domain.AccountType

import java.sql.Timestamp

class AccountBuilder {

    Long accountId = 0L
    String owner = 'test_owner'
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
        new AccountBuilder()
    }

    Account build() {
        Account account = new Account().with {
            accountId = this.accountId
            owner = this.owner
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
            it
        }
        account
    }

    AccountBuilder withOwner(String owner) {
        this.owner = owner
        this
    }

    AccountBuilder withAccountNameOwner(String accountNameOwner) {
        this.accountNameOwner = accountNameOwner
        this
    }

    AccountBuilder withAccountType(AccountType accountType) {
        this.accountType = accountType
        this
    }

    AccountBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        this
    }

    AccountBuilder withMoniker(String moniker) {
        this.moniker = moniker
        this
    }

    AccountBuilder withDateClosed(Timestamp dateClosed) {
        this.dateClosed = dateClosed
        this
    }

    AccountBuilder withFuture(BigDecimal future) {
        this.future = future
        this
    }

    AccountBuilder withOutstanding(BigDecimal outstanding) {
        this.outstanding = outstanding
        this
    }

    AccountBuilder withCleared(BigDecimal cleared) {
        this.cleared = cleared
        this
    }

    AccountBuilder withAccountId(Long accountId) {
        this.accountId = accountId
        this
    }

    AccountBuilder withDateAdded(Timestamp dateAdded) {
        this.dateAdded = dateAdded
        this
    }

    AccountBuilder withDateUpdated(Timestamp dateUpdated) {
        this.dateUpdated = dateUpdated
        this
    }

}
