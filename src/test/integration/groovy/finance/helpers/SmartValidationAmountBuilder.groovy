package finance.helpers

import finance.domain.TransactionState
import finance.domain.ValidationAmount
import groovy.util.logging.Slf4j
import java.sql.Timestamp

@Slf4j
class SmartValidationAmountBuilder {

    private String testOwner

    private Long accountId = 1L
    private Timestamp validationDate = new Timestamp(System.currentTimeMillis())
    private Boolean activeStatus = true
    private TransactionState transactionState = TransactionState.Cleared
    private BigDecimal amount = 0.00G

    private SmartValidationAmountBuilder(String testOwner) {
        this.testOwner = testOwner
    }

    static SmartValidationAmountBuilder builderForOwner(String testOwner) {
        return new SmartValidationAmountBuilder(testOwner)
    }

    ValidationAmount build() {
        ValidationAmount va = new ValidationAmount().with {
            validationId = 0L
            accountId = this.accountId
            validationDate = this.validationDate
            activeStatus = this.activeStatus
            transactionState = this.transactionState
            amount = this.amount
            return it
        }
        return va
    }

    ValidationAmount buildAndValidate() {
        ValidationAmount va = build()
        validateConstraints(va)
        return va
    }

    private void validateConstraints(ValidationAmount va) {
        if (va.accountId == null || va.accountId < 0L) {
            throw new IllegalStateException("accountId must be >= 0")
        }
        if (va.validationDate == null) {
            throw new IllegalStateException("validationDate must not be null")
        }
        if (va.transactionState == null) {
            throw new IllegalStateException("transactionState must not be null")
        }
        if (va.amount == null) {
            throw new IllegalStateException("amount must not be null")
        }
        // Precision (8,2)
        if (va.amount.scale() > 2) {
            throw new IllegalStateException("amount must have at most 2 decimal places")
        }
        BigDecimal max = new BigDecimal('99999999.99')
        if (va.amount.abs() > max) {
            throw new IllegalStateException("amount exceeds allowed precision (8,2)")
        }
        log.debug("ValidationAmount passed constraint validation: accountId=${va.accountId}, state=${va.transactionState}")
    }

    // Fluent API
    SmartValidationAmountBuilder withAccountId(Long accountId) {
        this.accountId = accountId
        return this
    }

    SmartValidationAmountBuilder withValidationDate(Timestamp validationDate) {
        this.validationDate = validationDate
        return this
    }

    SmartValidationAmountBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        return this
    }

    SmartValidationAmountBuilder withTransactionState(TransactionState transactionState) {
        this.transactionState = transactionState
        return this
    }

    SmartValidationAmountBuilder withAmount(BigDecimal amount) {
        this.amount = amount
        return this
    }

    // Convenience
    SmartValidationAmountBuilder asCleared() {
        this.transactionState = TransactionState.Cleared
        return this
    }

    SmartValidationAmountBuilder asOutstanding() {
        this.transactionState = TransactionState.Outstanding
        return this
    }

    SmartValidationAmountBuilder asFuture() {
        this.transactionState = TransactionState.Future
        return this
    }

    SmartValidationAmountBuilder asActive() {
        this.activeStatus = true
        return this
    }

    SmartValidationAmountBuilder asInactive() {
        this.activeStatus = false
        return this
    }
}

