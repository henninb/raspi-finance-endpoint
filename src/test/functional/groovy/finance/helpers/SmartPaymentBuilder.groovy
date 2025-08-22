package finance.helpers

import finance.domain.Payment
import groovy.util.logging.Slf4j
import java.sql.Date
import java.util.concurrent.atomic.AtomicInteger

@Slf4j
class SmartPaymentBuilder {

    private static final AtomicInteger COUNTER = new AtomicInteger(0)

    private String testOwner

    private String sourceAccount
    private String destinationAccount
    private BigDecimal amount = 0.00G
    private Date transactionDate = Date.valueOf('2023-01-01')
    private String guidSource = UUID.randomUUID().toString()
    private String guidDestination = UUID.randomUUID().toString()
    private Boolean activeStatus = true

    private SmartPaymentBuilder(String testOwner) {
        this.testOwner = testOwner
        this.sourceAccount = generateAlphaUnderscoreName('src')
        this.destinationAccount = generateAlphaUnderscoreName('dest')
    }

    static SmartPaymentBuilder builderForOwner(String testOwner) {
        return new SmartPaymentBuilder(testOwner)
    }

    private String generateAlphaUnderscoreName(String prefix) {
        // Must match ^[a-z-]*_[a-z]*$ and be 3-40 chars
        String counter = COUNTER.incrementAndGet().toString()
        String cleanPrefix = (prefix ?: 'acct').toLowerCase().replaceAll(/[^a-z-]/, '')
        if (cleanPrefix.isEmpty()) cleanPrefix = 'acct'

        String ownerPart = (testOwner ?: 'test').toLowerCase().replaceAll(/[^a-z]/, '')
        if (ownerPart.isEmpty()) ownerPart = 'test'

        String base = "${cleanPrefix}${counter}_${ownerPart}"
        if (base.length() > 40) {
            String shortOwner = ownerPart.length() > 10 ? ownerPart[0..9] : ownerPart
            base = "${cleanPrefix}${counter}_${shortOwner}"
        }
        if (base.length() < 3) base = "acc_${ownerPart}"
        return base.toLowerCase()
    }

    Payment build() {
        Payment payment = new Payment().with {
            paymentId = 0L
            sourceAccount = this.sourceAccount
            destinationAccount = this.destinationAccount
            amount = this.amount
            transactionDate = this.transactionDate
            guidSource = this.guidSource
            guidDestination = this.guidDestination
            activeStatus = this.activeStatus
            return it
        }
        return payment
    }

    Payment buildAndValidate() {
        Payment payment = build()
        validateConstraints(payment)
        return payment
    }

    private void validateConstraints(Payment payment) {
        // Account name constraints: 3-40, pattern ^[a-z-]*_[a-z]*$
        [payment.sourceAccount, payment.destinationAccount].each { acct ->
            if (acct == null || acct.length() < 3 || acct.length() > 40) {
                throw new IllegalStateException("Account '${acct}' violates length constraints (3-40 chars)")
            }
            if (!acct.matches(/^[a-z-]*_[a-z]*$/)) {
                throw new IllegalStateException("Account '${acct}' must match alpha_underscore pattern (letters/dashes_letters)")
            }
        }

        // Amount: non-negative, <= 8 integer digits, <= 2 fraction digits
        if (payment.amount == null || payment.amount < 0G) {
            throw new IllegalStateException("Amount must be non-negative")
        }
        if (payment.amount.scale() > 2) {
            throw new IllegalStateException("Amount must have at most 2 decimal places")
        }
        BigDecimal max = new BigDecimal('99999999.99')
        if (payment.amount > max) {
            throw new IllegalStateException("Amount exceeds allowed precision (8,2)")
        }

        // GUIDs must be UUID format
        def uuidRegex = /^[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}$/
        if (!(payment.guidSource =~ uuidRegex)) {
            throw new IllegalStateException("guidSource '${payment.guidSource}' must be UUID formatted")
        }
        if (!(payment.guidDestination =~ uuidRegex)) {
            throw new IllegalStateException("guidDestination '${payment.guidDestination}' must be UUID formatted")
        }

        // Date must be > 2000-01-01 (loose check)
        if (payment.transactionDate == null || !payment.transactionDate.after(Date.valueOf('2000-01-01'))) {
            throw new IllegalStateException("transactionDate must be greater than 2000-01-01")
        }

        log.debug("Payment passed constraint validation: ${payment}")
    }

    // Fluent API
    SmartPaymentBuilder withSourceAccount(String sourceAccount) {
        this.sourceAccount = sourceAccount?.toLowerCase()
        return this
    }

    SmartPaymentBuilder withDestinationAccount(String destinationAccount) {
        this.destinationAccount = destinationAccount?.toLowerCase()
        return this
    }

    SmartPaymentBuilder withUniqueAccounts(String srcPrefix = 'src', String destPrefix = 'dest') {
        this.sourceAccount = generateAlphaUnderscoreName(srcPrefix)
        this.destinationAccount = generateAlphaUnderscoreName(destPrefix)
        return this
    }

    SmartPaymentBuilder withAmount(BigDecimal amount) {
        this.amount = amount
        return this
    }

    SmartPaymentBuilder withTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate
        return this
    }

    SmartPaymentBuilder withGuidSource(String guidSource) {
        this.guidSource = guidSource?.toLowerCase()
        return this
    }

    SmartPaymentBuilder withGuidDestination(String guidDestination) {
        this.guidDestination = guidDestination?.toLowerCase()
        return this
    }

    SmartPaymentBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        return this
    }

    // Convenience
    SmartPaymentBuilder asActive() {
        this.activeStatus = true
        return this
    }

    SmartPaymentBuilder asInactive() {
        this.activeStatus = false
        return this
    }
}

