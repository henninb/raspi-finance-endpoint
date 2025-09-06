package finance.helpers

import finance.domain.Transfer
import groovy.util.logging.Slf4j
import java.sql.Date
import java.util.concurrent.atomic.AtomicInteger

@Slf4j
class SmartTransferBuilder {

    private static final AtomicInteger COUNTER = new AtomicInteger(0)

    private String testOwner

    private String sourceAccount
    private String destinationAccount
    private BigDecimal amount = 0.00G
    private Date transactionDate = Date.valueOf('2023-01-01')
    private String guidSource = UUID.randomUUID().toString()
    private String guidDestination = UUID.randomUUID().toString()
    private Boolean activeStatus = true

    private SmartTransferBuilder(String testOwner) {
        this.testOwner = testOwner
        this.sourceAccount = generateAlphaUnderscoreName('src')
        this.destinationAccount = generateAlphaUnderscoreName('dest')
    }

    static SmartTransferBuilder builderForOwner(String testOwner) {
        return new SmartTransferBuilder(testOwner)
    }

    private String generateAlphaUnderscoreName(String prefix) {
        // Must match ^[a-z-]*_[a-z]*$ and be 3-40 chars
        // No digits allowed, only letters and dashes before/after underscore
        int counter = COUNTER.incrementAndGet()
        String counterLetters = convertCounterToLetters(counter)

        String cleanPrefix = (prefix ?: 'acct').toLowerCase().replaceAll(/[^a-z-]/, '')
        if (cleanPrefix.isEmpty()) cleanPrefix = 'acct'

        String ownerPart = (testOwner ?: 'test').toLowerCase().replaceAll(/[^a-z]/, '')
        if (ownerPart.isEmpty()) ownerPart = 'test'

        String base = "${cleanPrefix}${counterLetters}_${ownerPart}"
        if (base.length() > 40) {
            String shortOwner = ownerPart.length() > 10 ? ownerPart[0..9] : ownerPart
            base = "${cleanPrefix}${counterLetters}_${shortOwner}"
        }
        if (base.length() < 3) base = "acc_${ownerPart}"
        return base.toLowerCase()
    }

    private String convertCounterToLetters(int counter) {
        // Convert counter to letters (1->a, 2->b, ..., 26->z, 27->aa, etc.)
        String result = ""
        while (counter > 0) {
            counter-- // Make it 0-based
            int charIndex = counter % 26
            char letterChar = (char)((int)'a' + charIndex)
            String letter = String.valueOf(letterChar)
            result = letter + result
            counter = (int)(counter / 26)
        }
        return result ?: "a"
    }

    Transfer build() {
        Transfer transfer = new Transfer().with {
            transferId = 0L
            sourceAccount = this.sourceAccount
            destinationAccount = this.destinationAccount
            amount = this.amount
            transactionDate = this.transactionDate
            guidSource = this.guidSource
            guidDestination = this.guidDestination
            activeStatus = this.activeStatus
            return it
        }
        return transfer
    }

    Transfer buildAndValidate() {
        Transfer transfer = build()
        validateConstraints(transfer)
        return transfer
    }

    private void validateConstraints(Transfer transfer) {
        // Account name constraints: 3-40, pattern ^[a-z-]*_[a-z]*$
        [transfer.sourceAccount, transfer.destinationAccount].each { acct ->
            if (acct == null || acct.length() < 3 || acct.length() > 40) {
                throw new IllegalStateException("Account '${acct}' violates length constraints (3-40 chars)")
            }
            if (!acct.matches(/^[a-z-]*_[a-z]*$/)) {
                throw new IllegalStateException("Account '${acct}' must match alpha_underscore pattern (letters/dashes_letters)")
            }
        }

        // Amount: non-negative, <= 8 integer digits, <= 2 fraction digits
        if (transfer.amount == null || transfer.amount < 0G) {
            throw new IllegalStateException("Amount must be non-negative")
        }
        if (transfer.amount.scale() > 2) {
            throw new IllegalStateException("Amount must have at most 2 decimal places")
        }
        BigDecimal max = new BigDecimal('999999.99')  // NUMERIC(8,2) = 6 integer + 2 decimal digits
        if (transfer.amount > max) {
            throw new IllegalStateException("Amount exceeds allowed precision (8,2)")
        }

        // GUIDs must be UUID format
        def uuidRegex = /^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$/
        if (!(transfer.guidSource =~ uuidRegex)) {
            throw new IllegalStateException("guidSource '${transfer.guidSource}' must be UUID formatted")
        }
        if (!(transfer.guidDestination =~ uuidRegex)) {
            throw new IllegalStateException("guidDestination '${transfer.guidDestination}' must be UUID formatted")
        }

        // Date must be > 2000-01-01 (loose check)
        if (transfer.transactionDate == null || !transfer.transactionDate.after(Date.valueOf('2000-01-01'))) {
            throw new IllegalStateException("transactionDate must be greater than 2000-01-01")
        }

        log.debug("Transfer passed constraint validation: ${transfer}")
    }

    // Fluent API
    SmartTransferBuilder withSourceAccount(String sourceAccount) {
        this.sourceAccount = sourceAccount?.toLowerCase()
        return this
    }

    SmartTransferBuilder withDestinationAccount(String destinationAccount) {
        this.destinationAccount = destinationAccount?.toLowerCase()
        return this
    }

    SmartTransferBuilder withUniqueAccounts(String srcPrefix = 'src', String destPrefix = 'dest') {
        this.sourceAccount = generateAlphaUnderscoreName(srcPrefix)
        this.destinationAccount = generateAlphaUnderscoreName(destPrefix)
        return this
    }

    SmartTransferBuilder withAmount(BigDecimal amount) {
        this.amount = amount
        return this
    }

    SmartTransferBuilder withTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate
        return this
    }

    SmartTransferBuilder withGuidSource(String guidSource) {
        this.guidSource = guidSource  // Don't convert UUIDs to lowercase
        return this
    }

    SmartTransferBuilder withGuidDestination(String guidDestination) {
        this.guidDestination = guidDestination  // Don't convert UUIDs to lowercase
        return this
    }

    SmartTransferBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        return this
    }

    // Convenience
    SmartTransferBuilder asActive() {
        this.activeStatus = true
        return this
    }

    SmartTransferBuilder asInactive() {
        this.activeStatus = false
        return this
    }
}

