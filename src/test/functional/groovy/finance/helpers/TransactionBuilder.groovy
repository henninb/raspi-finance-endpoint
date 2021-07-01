package finance.helpers

import finance.domain.*

import java.sql.Date

class TransactionBuilder {
    String guid = '4ea3be58-3993-abcd-88a2-4ffc7f1d73bd'
    Long accountId = 0
    AccountType accountType = AccountType.Credit
    String accountNameOwner = 'chase_brian'
    Date transactionDate = Date.valueOf('2020-12-01')
    Date dueDate = Date.valueOf('2020-12-02')
    String description = 'aliexpress.com'
    String category = 'online'
    BigDecimal amount = new BigDecimal('3.14')
    TransactionState transactionState = TransactionState.Cleared
    ReoccurringType reoccurringType = ReoccurringType.Undefined
    String notes = 'my note to you'
    Boolean activeStatus = true
    Long receiptImageId = null
    ReceiptImage receiptImage = null

    static TransactionBuilder builder() {
        return new TransactionBuilder()
    }

    Transaction build() {
        return new Transaction().with {
            guid = this.guid
            accountId = this.accountId
            accountType = this.accountType
            accountNameOwner = this.accountNameOwner
            transactionDate = this.transactionDate
            dueDate = this.dueDate
            description = this.description
            category = this.category
            amount = this.amount
            transactionState = this.transactionState
            reoccurringType = this.reoccurringType
            notes = this.notes
            activeStatus = this.activeStatus
            receiptImageId = this.receiptImageId
            receiptImage = this.receiptImage

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

    TransactionBuilder withDueDate(Date dueDate) {
        this.dueDate = dueDate
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

    TransactionBuilder withReceiptImageId(Long receiptImageId) {
        this.receiptImageId = receiptImageId
        return this
    }

    TransactionBuilder withImage() {
        this.receiptImage = ReceiptImageBuilder.builder().build()
        return this
    }
}
