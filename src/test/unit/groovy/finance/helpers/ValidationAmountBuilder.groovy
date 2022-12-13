package finance.helpers

import finance.domain.TransactionState
import finance.domain.ValidationAmount
import java.sql.Timestamp

class ValidationAmountBuilder {
    Long accountId = 1L
    TransactionState transactionState = TransactionState.Cleared
    Boolean activeStatus = true
    BigDecimal amount = new BigDecimal(0)
    Timestamp validationDate = new Timestamp(Calendar.instance.timeInMillis)

    static ValidationAmountBuilder builder() {
        return new ValidationAmountBuilder()
    }
    
    ValidationAmount build() {
        return new ValidationAmount().with {
            accountId = this.accountId
            amount = this.amount
            transactionState = this.transactionState
            activeStatus = this.activeStatus
            validationDate = this.validationDate
            return it
        }
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
}
