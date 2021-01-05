package finance.helpers

import finance.domain.AccountType
import finance.domain.ReoccurringType
import finance.domain.Transaction
import finance.domain.TransactionState

import java.sql.Date

class TransactionBuilder {
    String guid = '4ea3be58-3993-abcd-88a2-4ffc7f1d73bd'
    Long accountId = 0
    AccountType accountType = AccountType.Credit
    String accountNameOwner = 'chase_brian'
    Date transactionDate = Date.valueOf('2020-12-01')
    String description = 'aliexpress.com'
    String category = 'online'
    BigDecimal amount = 3.14G
    TransactionState transactionState = TransactionState.Cleared
    Boolean reoccurring = false
    ReoccurringType reoccurringType = ReoccurringType.Undefined
    String notes = 'my note to you'
    Boolean activeStatus = true

    static TransactionBuilder builder() {
        return new TransactionBuilder()
    }

    Transaction build() {
        return new Transaction().with() {
            it.guid = this.guid
            it.accountId = this.accountId
            it.accountType = this.accountType
            it.accountNameOwner = this.accountNameOwner
            it.transactionDate = this.transactionDate
            it.description = this.description
            it.category = this.category
            it.amount = this.amount
            it.transactionState = this.transactionState
            it.reoccurring = this.reoccurring
            it.reoccurringType = this.reoccurringType
            it.notes = this.notes
            it.activeStatus = this.activeStatus

            return it
        }
    }
    
    TransactionBuilder withGuid(String guid) {
        this.guid = guid
        return this
    }

    TransactionBuilder withAccountId(Long accountId) {
        this.accountId = accountId
        return this
    }

    TransactionBuilder withAccountType(AccountType accountType) {
        this.accountType = accountType
        return this
    }

    TransactionBuilder withAccountNameOwner(String accountNameOwner) {
        this.accountNameOwner = accountNameOwner
        return this
    }

    TransactionBuilder withTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate
        return this
    }

    TransactionBuilder withDescription(String description) {
        this.description = description
        return this
    }

    TransactionBuilder withCategory(String category) {
        this.category = category
        return this
    }

    TransactionBuilder withAmount(BigDecimal amount) {
        this.amount = amount
        return this
    }

    TransactionBuilder withTransactionState(TransactionState transactionState) {
        this.transactionState = transactionState
        return this
    }

    TransactionBuilder withReoccurring(Boolean reoccurring) {
        this.reoccurring = reoccurring
        return this
    }

    TransactionBuilder withReoccurringType(ReoccurringType reoccurringType) {
        this.reoccurringType = reoccurringType
        return this
    }

    TransactionBuilder withNotes(String notes) {
        this.notes = notes
        return this
    }

    TransactionBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        return this
    }
}
