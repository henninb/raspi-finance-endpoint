package finance.helpers

import finance.domain.Transaction
import finance.domain.AccountType

import java.math.RoundingMode
import java.sql.Timestamp
import java.sql.Date

class TransactionBuilder {

    Long transactionId = 1002
    String guid = '4ea3be58-3993-46de-88a2-4ffc7f1d73bd'
    Long accountId = 0
    AccountType accountType = AccountType.Credit
    String accountNameOwner = 'chase_brian'
    Date transactionDate = new Date(1553645394)
    String description = 'aliexpress.com'
    String category = 'online'
    BigDecimal amount = new BigDecimal(3.14).setScale(2, RoundingMode.HALF_UP)
    Integer cleared = 1
    Boolean reoccurring = false
    String notes = 'my note to you'
    Timestamp dateUpdated = new Timestamp(1553645394000)
    Timestamp dateAdded = new Timestamp(1553645394000)
    String sha256 = '963e35c37ea59f3f6fa35d72fb0ba47e1e1523fae867eeeb7ead64b55ff22b77'

    static TransactionBuilder builder() {
        return new TransactionBuilder()
    }

    Transaction build() {
        Transaction transaction = new Transaction()
        transaction.transactionId = transactionId
        transaction.guid = guid
        transaction.accountId = accountId
        transaction.accountType = accountType
        transaction.accountNameOwner = accountNameOwner
        transaction.transactionDate = transactionDate
        transaction.description = description
        transaction.category = category
        transaction.amount = amount
        transaction.cleared = cleared
        transaction.reoccurring = reoccurring
        transaction.notes = notes
        transaction.dateAdded = dateAdded
        transaction.dateUpdated = dateUpdated
        transaction.sha256 = sha256
        return transaction
    }

    TransactionBuilder transactionId(transactionId) {
        this.transactionId = transactionId
        return this
    }

    TransactionBuilder guid(guid) {
        this.guid = guid
        return this
    }

    TransactionBuilder accountId(accountId) {
        this.accountId = accountId
        return this
    }

    TransactionBuilder accountType(accountType) {
        this.accountType = accountType
        return this
    }

    TransactionBuilder accountNameOwner(accountNameOwner) {
        this.accountNameOwner = accountNameOwner
        return this
    }

    TransactionBuilder transactionDate(transactionDate) {
        this.transactionDate = transactionDate
        return this
    }

    TransactionBuilder description(description) {
        this.description = description
        return this
    }

    TransactionBuilder category(category) {
        this.category = category
        return this
    }

    TransactionBuilder amount(amount) {
        this.amount = amount
        return this
    }

    TransactionBuilder cleared(cleared) {
        this.cleared = cleared
        return this
    }

    TransactionBuilder reoccurring(reoccurring) {
        this.reoccurring = reoccurring
        return this
    }

    TransactionBuilder notes(notes) {
        this.notes = notes
        return this;
    }

    TransactionBuilder dateUpdated(dateUpdated) {
        this.dateUpdated = dateUpdated
        return this
    }

    TransactionBuilder dateAdded(dateAdded) {
        this.dateAdded = dateAdded
        return this
    }

    TransactionBuilder sha256(sha256) {
        this.sha256 = sha256
        return this
    }
}
