package finance.helpers

import finance.domain.Account
import finance.domain.AccountType
import groovy.util.logging.Slf4j
import java.sql.Timestamp
import java.util.concurrent.atomic.AtomicInteger

@Slf4j
class SmartAccountBuilder {

    private static final AtomicInteger COUNTER = new AtomicInteger(0)

    private String testOwner
    private String accountNameOwner
    private AccountType accountType = AccountType.Credit
    private Boolean activeStatus = true
    private String moniker = '0000'
    private BigDecimal future = new BigDecimal(0)
    private BigDecimal outstanding = new BigDecimal(0)
    private BigDecimal cleared = new BigDecimal(0)
    private Timestamp dateClosed = new Timestamp(0)
    private Timestamp validationDate = new Timestamp(System.currentTimeMillis())

    private SmartAccountBuilder(String testOwner) {
        this.testOwner = testOwner
        // Generate a unique, constraint-compliant account name
        this.accountNameOwner = generateUniqueAccountName()
    }

    static SmartAccountBuilder builderForOwner(String testOwner) {
        return new SmartAccountBuilder(testOwner)
    }

    private String generateUniqueAccountName() {
        // ALPHA_UNDERSCORE_PATTERN = "^[a-z-]*_[a-z]*$" (letters/dashes + single underscore + letters)
        String counter = COUNTER.incrementAndGet().toString()
        String ownerPart = testOwner.replaceAll(/[^a-z]/, '') // Keep only letters

        if (ownerPart.isEmpty()) {
            ownerPart = "test"
        }

        // Create base name in correct pattern: letters_letters
        String baseName = "account${counter}_${ownerPart}"

        // Ensure length constraints (3-40 chars)
        if (baseName.length() > 40) {
            String shortOwner = ownerPart.length() > 8 ? ownerPart[0..7] : ownerPart
            baseName = "acc${counter}_${shortOwner}"
        }

        if (baseName.length() < 3) {
            baseName = "acc_${ownerPart}"
        }

        // Final validation - must match ^[a-z-]*_[a-z]*$
        String cleaned = baseName.toLowerCase()

        log.debug("Generated account name: ${cleaned} for test owner: ${testOwner}")
        return cleaned
    }

    Account build() {
        Account account = new Account().with {
            accountNameOwner = this.accountNameOwner
            accountType = this.accountType
            activeStatus = this.activeStatus
            moniker = this.moniker
            future = this.future
            outstanding = this.outstanding
            cleared = this.cleared
            dateClosed = this.dateClosed
            validationDate = this.validationDate
            return it
        }
        return account
    }

    Account buildAndValidate() {
        Account account = build()
        validateConstraints(account)
        return account
    }

    private void validateConstraints(Account account) {
        // Validate account name constraints
        if (account.accountNameOwner.length() < 3 || account.accountNameOwner.length() > 40) {
            throw new IllegalStateException("Account name '${account.accountNameOwner}' violates length constraints (3-40 chars)")
        }

        // ALPHA_UNDERSCORE_PATTERN = "^[a-z-]*_[a-z]*$"
        if (!account.accountNameOwner.matches(/^[a-z-]*_[a-z]*$/)) {
            throw new IllegalStateException("Account name '${account.accountNameOwner}' violates alpha_underscore pattern (must be letters/dashes_letters)")
        }

        // Validate moniker constraint (should be 4 digits)
        if (!account.moniker.matches(/^\d{4}$/)) {
            throw new IllegalStateException("Moniker '${account.moniker}' must be exactly 4 digits")
        }

        log.debug("Account passed constraint validation: ${account.accountNameOwner}")
    }

    // Fluent API methods
    SmartAccountBuilder withAccountNameOwner(String accountNameOwner) {
        this.accountNameOwner = accountNameOwner
        return this
    }

    SmartAccountBuilder withUniqueAccountName(String prefix = "test") {
        this.accountNameOwner = generateUniqueAccountName(prefix)
        return this
    }

    private String generateUniqueAccountName(String prefix) {
        // ALPHA_UNDERSCORE_PATTERN = "^[a-z-]*_[a-z]*$" (letters/dashes + single underscore + letters)
        String cleanPrefix = prefix.replaceAll(/[^a-z]/, '') // Keep only letters
        String ownerPart = testOwner.replaceAll(/[^a-z]/, '') // Keep only letters

        if (cleanPrefix.isEmpty()) {
            cleanPrefix = "account"
        }

        if (ownerPart.isEmpty()) {
            ownerPart = "test"
        }

        // Add unique suffix using UUID to ensure true uniqueness
        String uuid = UUID.randomUUID().toString().replaceAll(/[^a-z]/, '').take(4)
        String baseName = "${cleanPrefix}${uuid}_${ownerPart}"

        // Ensure length constraints (3-40 chars)
        if (baseName.length() > 40) {
            String shortOwner = ownerPart.length() > 8 ? ownerPart[0..7] : ownerPart
            baseName = "${cleanPrefix}${uuid}_${shortOwner}"
        }

        if (baseName.length() < 3) {
            baseName = "acc_${ownerPart}"
        }

        return baseName.toLowerCase()
    }

    SmartAccountBuilder withAccountType(AccountType accountType) {
        this.accountType = accountType
        return this
    }

    SmartAccountBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        return this
    }

    SmartAccountBuilder withMoniker(String moniker) {
        if (!moniker.matches(/^\d{4}$/)) {
            log.warn("Invalid moniker '${moniker}', using default '0000'")
            this.moniker = '0000'
        } else {
            this.moniker = moniker
        }
        return this
    }

    SmartAccountBuilder withDateClosed(Timestamp dateClosed) {
        this.dateClosed = dateClosed
        return this
    }

    SmartAccountBuilder withValidationDate(Timestamp validationDate) {
        this.validationDate = validationDate
        return this
    }

    SmartAccountBuilder withFuture(BigDecimal future) {
        this.future = future
        return this
    }

    SmartAccountBuilder withOutstanding(BigDecimal outstanding) {
        this.outstanding = outstanding
        return this
    }

    SmartAccountBuilder withCleared(BigDecimal cleared) {
        this.cleared = cleared
        return this
    }

    // Convenience methods for common scenarios
    SmartAccountBuilder asCredit() {
        this.accountType = AccountType.Credit
        return this
    }

    SmartAccountBuilder asDebit() {
        this.accountType = AccountType.Debit
        return this
    }

    SmartAccountBuilder asInactive() {
        this.activeStatus = false
        return this
    }

    SmartAccountBuilder asActive() {
        this.activeStatus = true
        return this
    }

    SmartAccountBuilder withBalances(BigDecimal cleared, BigDecimal outstanding = new BigDecimal(0), BigDecimal future = new BigDecimal(0)) {
        this.cleared = cleared
        this.outstanding = outstanding
        this.future = future
        return this
    }
}