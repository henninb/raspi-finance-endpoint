package finance.helpers

import finance.domain.TransactionState
import finance.domain.ValidationAmount
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.*

class ValidationAmountBuilder {
    Long validationId = 0L
    Long accountId = 1L
    TransactionState transactionState = TransactionState.Cleared
    Boolean activeStatus = true
    BigDecimal amount = new BigDecimal("0.00")
    Timestamp validationDate = new Timestamp(Calendar.getInstance().time.time)
    Timestamp dateAdded = new Timestamp(Calendar.getInstance().time.time)
    Timestamp dateUpdated = new Timestamp(Calendar.getInstance().time.time)

    static ValidationAmountBuilder builder() {
        return new ValidationAmountBuilder()
    }

    ValidationAmount build() {
        return new ValidationAmount().with {
            validationId = this.validationId
            accountId = this.accountId
            amount = this.amount
            transactionState = this.transactionState
            activeStatus = this.activeStatus
            validationDate = this.validationDate
            dateAdded = this.dateAdded
            dateUpdated = this.dateUpdated
            return it
        }
    }

    ValidationAmountBuilder withValidationId(Long validationId) {
        this.validationId = validationId
        return this
    }

    ValidationAmountBuilder withValidationDate(Timestamp validationDate) {
        this.validationDate = validationDate
        return this
    }

    ValidationAmountBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        return this
    }

    ValidationAmountBuilder withAccountId(Long accountId) {
        this.accountId = accountId
        return this
    }

    ValidationAmountBuilder withAmount(BigDecimal amount) {
        this.amount = amount
        return this
    }

    ValidationAmountBuilder withTransactionState(TransactionState transactionState) {
        this.transactionState = transactionState
        return this
    }

    ValidationAmountBuilder withDateAdded(Timestamp dateAdded) {
        this.dateAdded = dateAdded
        return this
    }

    ValidationAmountBuilder withDateUpdated(Timestamp dateUpdated) {
        this.dateUpdated = dateUpdated
        return this
    }
}
