package finance.helpers

import finance.domain.Account
import finance.domain.AccountType

import java.sql.Timestamp

class AccountBuilder {

    String accountNameOwner = "chase_brian"
    AccountType accountType = AccountType.Credit
    Boolean activeStatus = true
    String moniker = "0000"
    //BigDecimal totals: Double = 0.0,
    //BigDecimal totalsBalanced: BigDecimal = 0.0,
    Timestamp dateClosed = new Timestamp(0)
    Timestamp dateUpdated = new Timestamp(1553645394000)
    Timestamp dateAdded = new Timestamp(1553645394000)


    static AccountBuilder builder() {
        return new AccountBuilder()
    }

    Account build() {
        Account account = new Account()
        account.accountNameOwner = accountNameOwner
        account.accountType = accountType
        account.activeStatus = activeStatus
        account.moniker = moniker
        account.dateClosed = dateClosed
        account.dateAdded = dateAdded
        account.dateUpdated = dateUpdated
        return account
    }

    AccountBuilder accountNameOwner(accountNameOwner) {
        this.accountNameOwner = accountNameOwner
        return this
    }

    AccountBuilder accountType(accountType) {
        this.accountType = accountType
        return this
    }
    AccountBuilder activeStatus(activeStatus) {
        this.activeStatus = activeStatus
        return this
    }

    AccountBuilder moniker(moniker) {
        this.moniker = moniker
        return this
    }

    AccountBuilder dateClosed(dateClosed) {
        this.dateClosed = dateClosed
        return this
    }

    AccountBuilder dateUpdated(dateUpdated) {
        this.dateUpdated = dateUpdated
        return this
    }

    AccountBuilder dateAdded(dateAdded) {
        this.dateAdded = dateAdded
        return this
    }
}
