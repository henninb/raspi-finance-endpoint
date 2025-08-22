package finance.helpers

import finance.domain.PendingTransaction
import groovy.util.logging.Slf4j
import java.math.BigDecimal
import java.sql.Date
import java.util.concurrent.atomic.AtomicInteger

@Slf4j
class SmartPendingTransactionBuilder {

    private static final AtomicInteger COUNTER = new AtomicInteger(0)

    private String testOwner

    private String accountNameOwner
    private Date transactionDate
    private String description
    private BigDecimal amount = 0.00G
    private String reviewStatus = 'pending'
    private String owner

    private SmartPendingTransactionBuilder(String testOwner) {
        this.testOwner = testOwner
        this.accountNameOwner = generateUniqueAccountName('account')
        // within last year, always > 2000-01-01
        this.transactionDate = new Date(System.currentTimeMillis() - (Math.random() * 180 * 24 * 60 * 60 * 1000L) as Long)
        this.description = generateUniqueDescription('pending')
        this.owner = testOwner
    }

    static SmartPendingTransactionBuilder builderForOwner(String testOwner) {
        return new SmartPendingTransactionBuilder(testOwner)
    }

    PendingTransaction build() {
        PendingTransaction pt = new PendingTransaction().with {
            pendingTransactionId = 0L
            accountNameOwner = this.accountNameOwner
            transactionDate = this.transactionDate
            description = this.description
            amount = this.amount
            reviewStatus = this.reviewStatus
            owner = this.owner
            return it
        }
        return pt
    }

    PendingTransaction buildAndValidate() {
        PendingTransaction pt = build()
        validateConstraints(pt)
        return pt
    }

    private void validateConstraints(PendingTransaction pt) {
        // accountNameOwner: 3-40, ^[a-z-]*_[a-z]*$
        if (pt.accountNameOwner == null || pt.accountNameOwner.length() < 3 || pt.accountNameOwner.length() > 40) {
            throw new IllegalStateException("accountNameOwner '${pt.accountNameOwner}' violates length constraints (3-40)")
        }
        if (!pt.accountNameOwner.matches(/^[a-z-]*_[a-z]*$/)) {
            throw new IllegalStateException("accountNameOwner '${pt.accountNameOwner}' must match alpha_underscore pattern (letters/dashes_letters)")
        }

        // transactionDate: must be > 2000-01-01
        if (pt.transactionDate == null || !pt.transactionDate.after(Date.valueOf('2000-01-01'))) {
            throw new IllegalStateException("transactionDate must be greater than 2000-01-01")
        }

        // description: 1-75 ASCII
        if (pt.description == null || pt.description.length() < 1 || pt.description.length() > 75) {
            throw new IllegalStateException("description length must be 1-75 characters")
        }
        if (!pt.description.matches(/^[\x20-\x7E]*$/)) {
            throw new IllegalStateException("description must be ASCII characters")
        }

        // amount: Digits(12,2)
        if (pt.amount == null) {
            throw new IllegalStateException("amount must not be null")
        }
        if (pt.amount.scale() > 2) {
            throw new IllegalStateException("amount must have at most 2 decimal places")
        }
        BigDecimal max = new BigDecimal('999999999999.99')
        if (pt.amount.abs() > max) {
            throw new IllegalStateException("amount exceeds allowed precision (12,2)")
        }

        log.debug("PendingTransaction passed constraint validation: acct=${pt.accountNameOwner}, amt=${pt.amount}")
    }

    // Helpers for unique field generation
    private String generateUniqueAccountName(String prefix) {
        // Must match ^[a-z-]*_[a-z]*$
        String cleanPrefix = (prefix ?: 'account').toLowerCase().replaceAll(/[^a-z-]/, '')
        if (cleanPrefix.isEmpty()) cleanPrefix = 'account'

        String ownerPart = (testOwner ?: 'test').toLowerCase().replaceAll(/[^a-z]/, '')
        if (ownerPart.isEmpty()) ownerPart = 'test'

        String base = "${cleanPrefix}_${ownerPart}"
        if (base.length() > 40) {
            String shortOwner = ownerPart.length() > 8 ? ownerPart[0..7] : ownerPart
            base = "${cleanPrefix}_${shortOwner}"
        }
        if (base.length() < 3) base = "acc_${ownerPart}"
        return base.toLowerCase()
    }

    private String generateUniqueDescription(String prefix) {
        String base = "${prefix}_${COUNTER.incrementAndGet()}_${testOwner}"
        if (base.length() > 75) {
            String shortOwner = testOwner.length() > 8 ? testOwner[0..7] : testOwner
            base = "${prefix}_${COUNTER.get()}_${shortOwner}"
        }
        return base.toLowerCase().replaceAll(/[^\x20-\x7E]/, '_')
    }

    // Fluent API
    SmartPendingTransactionBuilder withAccountNameOwner(String accountNameOwner) {
        this.accountNameOwner = accountNameOwner?.toLowerCase()
        return this
    }

    SmartPendingTransactionBuilder withUniqueAccountName(String prefix = 'account') {
        this.accountNameOwner = generateUniqueAccountName(prefix)
        return this
    }

    SmartPendingTransactionBuilder withTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate
        return this
    }

    SmartPendingTransactionBuilder withDescription(String description) {
        this.description = description
        return this
    }

    SmartPendingTransactionBuilder withUniqueDescription(String prefix = 'pending') {
        this.description = generateUniqueDescription(prefix)
        return this
    }

    SmartPendingTransactionBuilder withAmount(BigDecimal amount) {
        this.amount = amount
        return this
    }

    SmartPendingTransactionBuilder withAmount(String amount) {
        this.amount = new BigDecimal(amount)
        return this
    }

    SmartPendingTransactionBuilder withReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus
        return this
    }

    SmartPendingTransactionBuilder withOwner(String owner) {
        this.owner = owner
        return this
    }

    // Convenience
    SmartPendingTransactionBuilder asPending() {
        this.reviewStatus = 'pending'
        return this
    }

    SmartPendingTransactionBuilder asReviewed() {
        this.reviewStatus = 'reviewed'
        return this
    }

    SmartPendingTransactionBuilder asIgnored() {
        this.reviewStatus = 'ignored'
        return this
    }
}

