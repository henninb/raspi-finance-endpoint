package finance.helpers

import finance.domain.*
import groovy.util.logging.Slf4j
import java.math.BigDecimal
import java.sql.Date
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

@Slf4j
class SmartTransactionBuilder {

    private static final AtomicInteger COUNTER = new AtomicInteger(0)

    private String testOwner
    private String guid
    private Long accountId = 0L
    private AccountType accountType = AccountType.Credit
    private TransactionType transactionType = TransactionType.Expense
    private String accountNameOwner
    private LocalDate transactionDate
    private LocalDate dueDate
    private String description
    private String category
    private BigDecimal amount
    private TransactionState transactionState = TransactionState.Cleared
    private ReoccurringType reoccurringType = ReoccurringType.Undefined
    private String notes = ""
    private Boolean activeStatus = true
    private Long receiptImageId = null
    private ReceiptImage receiptImage = null

    private SmartTransactionBuilder(String testOwner) {
        this.testOwner = testOwner
        // Initialize with constraint-compliant defaults
        this.guid = UUID.randomUUID().toString()
        this.accountNameOwner = generateConstraintCompliantAccountName()
        long randomDays = (long) (Math.random() * 365)
        this.transactionDate = LocalDate.now().minusDays(randomDays)
        this.dueDate = this.transactionDate.plusDays(7)
        this.description = generateConstraintCompliantDescription()
        this.category = generateConstraintCompliantCategory()
        this.amount = generateValidAmount()
    }

    static SmartTransactionBuilder builderForOwner(String testOwner) {
        return new SmartTransactionBuilder(testOwner)
    }

    private String generateConstraintCompliantAccountName() {
        // accountNameOwner: Size(min=3, max=40) + ALPHA_UNDERSCORE_PATTERN + lowercase
        // ALPHA_UNDERSCORE_PATTERN = "^[a-z-]*_[a-z]*$" (letters/dashes + single underscore + letters)

        // Create base name in correct pattern: letters_letters
        String baseAccountName = "account"
        String ownerPart = testOwner.replaceAll(/[^a-z]/, '') // Keep only letters

        if (ownerPart.isEmpty()) {
            ownerPart = "test"
        }

        String baseName = "${baseAccountName}_${ownerPart}"

        // Ensure length constraints (3-40 chars)
        if (baseName.length() > 40) {
            String shortOwner = ownerPart.length() > 8 ? ownerPart[0..7] : ownerPart
            baseName = "${baseAccountName}_${shortOwner}"
        }

        if (baseName.length() < 3) {
            baseName = "acc_${ownerPart}"
        }

        // Final validation - must match ^[a-z-]*_[a-z]*$
        String cleaned = baseName.toLowerCase()

        log.debug("Generated account name: ${cleaned} for test owner: ${testOwner}")
        return cleaned
    }

    private String generateConstraintCompliantDescription() {
        // description: Size(min=1, max=75) + ASCII_PATTERN + lowercase
        String baseDescription = "transaction_${COUNTER.incrementAndGet()}_${testOwner}"

        if (baseDescription.length() > 75) {
            String shortOwner = testOwner.length() > 8 ? testOwner[0..7] : testOwner
            baseDescription = "txn_${COUNTER.get()}_${shortOwner}"
        }

        if (baseDescription.length() < 1) {
            baseDescription = "transaction${COUNTER.get()}"
        }

        // ASCII pattern compliance (basic ASCII characters)
        String cleaned = baseDescription.toLowerCase().replaceAll(/[^\x20-\x7E]/, '_')

        log.debug("Generated description: ${cleaned} for test owner: ${testOwner}")
        return cleaned
    }

    private String generateConstraintCompliantCategory() {
        // category: Size(max=50) + ALPHA_NUMERIC_NO_SPACE_PATTERN + lowercase
        // ALPHA_NUMERIC_NO_SPACE_PATTERN = "^[a-z0-9_-]*$" (letters, numbers, underscores, dashes)

        String baseCategory = "online"  // Use simple, known-good category name
        String ownerPart = testOwner.replaceAll(/[^a-z0-9]/, '') // Keep only alphanumeric

        if (ownerPart.length() > 5) {
            ownerPart = ownerPart[0..4]  // Keep short to avoid 50 char limit
        }

        String category = "${baseCategory}${ownerPart}"

        // Ensure length constraint (max 50 chars)
        if (category.length() > 50) {
            category = "online"
        }

        // Ensure not empty
        if (category.isEmpty()) {
            category = "online"
        }

        String cleaned = category.toLowerCase()

        log.debug("Generated category: ${cleaned} for test owner: ${testOwner}")
        return cleaned
    }

    private BigDecimal generateValidAmount() {
        // amount: Digits(integer=8, fraction=2) - max 99,999,999.99
        Random random = new Random()
        BigDecimal amount = new BigDecimal(String.format("%.2f", random.nextDouble() * 1000 + 1))
        return amount
    }

    Transaction build() {
        Transaction transaction = new Transaction().with {
            guid = this.guid
            accountId = this.accountId
            accountType = this.accountType
            transactionType = this.transactionType
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
        return transaction
    }

    Transaction buildAndValidate() {
        Transaction transaction = build()
        validateConstraints(transaction)
        return transaction
    }

    private void validateConstraints(Transaction transaction) {
        // Validate GUID (UUID pattern)
        if (!transaction.guid.matches(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/)) {
            throw new IllegalStateException("Transaction GUID '${transaction.guid}' violates UUID pattern constraint")
        }

        // Validate accountNameOwner (3-40 chars, alpha_underscore)
        if (transaction.accountNameOwner.length() < 3 || transaction.accountNameOwner.length() > 40) {
            throw new IllegalStateException("Account name owner '${transaction.accountNameOwner}' violates length constraints (3-40 chars)")
        }
        // ALPHA_UNDERSCORE_PATTERN = "^[a-z-]*_[a-z]*$"
        if (!transaction.accountNameOwner.matches(/^[a-z-]*_[a-z]*$/)) {
            throw new IllegalStateException("Account name owner '${transaction.accountNameOwner}' violates alpha_underscore pattern (must be letters/dashes_letters)")
        }

        // Validate description (1-75 chars, ASCII)
        if (transaction.description.length() < 1 || transaction.description.length() > 75) {
            throw new IllegalStateException("Description '${transaction.description}' violates length constraints (1-75 chars)")
        }
        if (!transaction.description.matches(/^[\x20-\x7E]*$/)) {
            throw new IllegalStateException("Description '${transaction.description}' violates ASCII pattern")
        }

        // Validate category (max 50 chars, alphanumeric no space)
        if (transaction.category.length() > 50) {
            throw new IllegalStateException("Category '${transaction.category}' violates length constraints (max 50 chars)")
        }
        // ALPHA_NUMERIC_NO_SPACE_PATTERN = "^[a-z0-9_-]*$"
        if (!transaction.category.matches(/^[a-z0-9_-]*$/)) {
            throw new IllegalStateException("Category '${transaction.category}' violates alphanumeric no space pattern (letters/numbers/underscores/dashes)")
        }

        // Validate amount (max 8 integer digits, 2 fraction digits)
        BigDecimal maxValue = new BigDecimal("99999999.99")
        if (transaction.amount.compareTo(maxValue) > 0) {
            throw new IllegalStateException("Amount '${transaction.amount}' exceeds maximum value (99999999.99)")
        }
        if (transaction.amount.scale() > 2) {
            throw new IllegalStateException("Amount '${transaction.amount}' has more than 2 decimal places")
        }

        // Validate notes (max 100 chars, ASCII)
        if (transaction.notes.length() > 100) {
            throw new IllegalStateException("Notes '${transaction.notes}' violates length constraints (max 100 chars)")
        }
        if (!transaction.notes.matches(/^[\x20-\x7E]*$/)) {
            throw new IllegalStateException("Notes '${transaction.notes}' violates ASCII pattern")
        }

        log.debug("Transaction passed all constraint validations: ${transaction.guid}")
    }

    // Fluent API methods
    SmartTransactionBuilder withGuid(String guid) {
        this.guid = guid
        return this
    }

    SmartTransactionBuilder withUniqueGuid() {
        this.guid = UUID.randomUUID().toString()
        return this
    }

    SmartTransactionBuilder withAccountId(Long accountId) {
        this.accountId = accountId
        return this
    }

    SmartTransactionBuilder withAccountType(AccountType accountType) {
        this.accountType = accountType
        return this
    }

    SmartTransactionBuilder withTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType
        return this
    }

    SmartTransactionBuilder withAccountNameOwner(String accountNameOwner) {
        this.accountNameOwner = accountNameOwner.toLowerCase()
        return this
    }

    SmartTransactionBuilder withUniqueAccountName(String prefix = "account") {
        this.accountNameOwner = generateUniqueAccountName(prefix)
        return this
    }

    private String generateUniqueAccountName(String prefix) {
        // Must match ALPHA_UNDERSCORE_PATTERN = "^[a-z-]*_[a-z]*$"
        String cleanPrefix = prefix.replaceAll(/[^a-z]/, '') // Keep only letters
        String ownerPart = testOwner.replaceAll(/[^a-z]/, '') // Keep only letters

        if (cleanPrefix.isEmpty()) {
            cleanPrefix = "account"
        }

        if (ownerPart.isEmpty()) {
            ownerPart = "test"
        }

        String baseName = "${cleanPrefix}_${ownerPart}"

        // Ensure length constraints (3-40 chars)
        if (baseName.length() > 40) {
            String shortOwner = ownerPart.length() > 8 ? ownerPart[0..7] : ownerPart
            baseName = "${cleanPrefix}_${shortOwner}"
        }

        if (baseName.length() < 3) {
            baseName = "acc_${ownerPart}"
        }

        return baseName.toLowerCase()
    }

    SmartTransactionBuilder withTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate?.toLocalDate()
        return this
    }

    SmartTransactionBuilder withTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate
        return this
    }

    SmartTransactionBuilder withDueDate(Date dueDate) {
        this.dueDate = dueDate?.toLocalDate()
        return this
    }

    SmartTransactionBuilder withDueDate(LocalDate dueDate) {
        this.dueDate = dueDate
        return this
    }

    SmartTransactionBuilder withDescription(String description) {
        this.description = description.toLowerCase()
        return this
    }

    SmartTransactionBuilder withUniqueDescription(String prefix = "desc") {
        this.description = generateUniqueDescription(prefix)
        return this
    }

    private String generateUniqueDescription(String prefix) {
        String baseDescription = "${prefix}_${COUNTER.incrementAndGet()}_${testOwner}"

        if (baseDescription.length() > 75) {
            String shortOwner = testOwner.length() > 8 ? testOwner[0..7] : testOwner
            baseDescription = "${prefix}_${COUNTER.get()}_${shortOwner}"
        }

        return baseDescription.toLowerCase().replaceAll(/[^\x20-\x7E]/, '_')
    }

    SmartTransactionBuilder withCategory(String category) {
        this.category = category.toLowerCase()
        return this
    }

    SmartTransactionBuilder withUniqueCategory(String prefix = "cat") {
        this.category = generateUniqueCategory(prefix)
        return this
    }

    private String generateUniqueCategory(String prefix) {
        // Must match ALPHA_NUMERIC_NO_SPACE_PATTERN = "^[a-z0-9_-]*$"
        String cleanPrefix = prefix.replaceAll(/[^a-z0-9]/, '') // Keep only alphanumeric
        String ownerPart = testOwner.replaceAll(/[^a-z0-9]/, '') // Keep only alphanumeric

        if (cleanPrefix.isEmpty()) {
            cleanPrefix = "online"
        }

        if (ownerPart.length() > 5) {
            ownerPart = ownerPart[0..4]  // Keep short
        }

        String category = "${cleanPrefix}${ownerPart}"

        // Ensure length constraint (max 50 chars)
        if (category.length() > 50) {
            category = cleanPrefix
        }

        // Ensure not empty
        if (category.isEmpty()) {
            category = "online"
        }

        return category.toLowerCase()
    }

    SmartTransactionBuilder withAmount(BigDecimal amount) {
        this.amount = amount
        return this
    }

    SmartTransactionBuilder withAmount(String amount) {
        this.amount = new BigDecimal(amount)
        return this
    }

    SmartTransactionBuilder withRandomAmount() {
        this.amount = generateValidAmount()
        return this
    }

    SmartTransactionBuilder withTransactionState(TransactionState transactionState) {
        this.transactionState = transactionState
        return this
    }

    SmartTransactionBuilder withReoccurringType(ReoccurringType reoccurringType) {
        this.reoccurringType = reoccurringType
        return this
    }

    SmartTransactionBuilder withNotes(String notes) {
        this.notes = notes.toLowerCase()
        return this
    }

    SmartTransactionBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        return this
    }

    SmartTransactionBuilder withReceiptImageId(Long receiptImageId) {
        this.receiptImageId = receiptImageId
        return this
    }

    SmartTransactionBuilder withReceiptImage(ReceiptImage receiptImage) {
        this.receiptImage = receiptImage
        return this
    }

    // Convenience methods for common scenarios
    SmartTransactionBuilder asActive() {
        this.activeStatus = true
        return this
    }

    SmartTransactionBuilder asInactive() {
        this.activeStatus = false
        return this
    }

    SmartTransactionBuilder asExpense() {
        this.transactionType = TransactionType.Expense
        return this
    }

    SmartTransactionBuilder asIncome() {
        this.transactionType = TransactionType.Income
        return this
    }

    SmartTransactionBuilder asCredit() {
        this.accountType = AccountType.Credit
        return this
    }

    SmartTransactionBuilder asDebit() {
        this.accountType = AccountType.Debit
        return this
    }

    SmartTransactionBuilder asCleared() {
        this.transactionState = TransactionState.Cleared
        return this
    }

    SmartTransactionBuilder asPending() {
        this.transactionState = TransactionState.Outstanding
        return this
    }

    SmartTransactionBuilder asOutstanding() {
        this.transactionState = TransactionState.Outstanding
        return this
    }

    SmartTransactionBuilder asFuture() {
        this.transactionState = TransactionState.Future
        return this
    }

    // Business domain convenience methods
    SmartTransactionBuilder asOnlineTransaction() {
        this.category = generateUniqueCategory("online")
        this.description = generateUniqueDescription("online_purchase")
        return this
    }

    SmartTransactionBuilder asGroceryTransaction() {
        this.category = generateUniqueCategory("groceries")
        this.description = generateUniqueDescription("grocery_store")
        return this
    }

    SmartTransactionBuilder asUtilityTransaction() {
        this.category = generateUniqueCategory("utilities")
        this.description = generateUniqueDescription("utility_payment")
        return this
    }

    SmartTransactionBuilder asRestaurantTransaction() {
        this.category = generateUniqueCategory("dining")
        this.description = generateUniqueDescription("restaurant")
        return this
    }
}
