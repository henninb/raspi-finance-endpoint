package finance.controllers

import finance.domain.Transaction
import finance.helpers.SmartTransactionBuilder
import finance.helpers.TransactionTestContext
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared

@ActiveProfiles("func")
class TransactionControllerFunctionalSpec extends BaseControllerFunctionalSpec {

    @Shared
    protected String endpointName = 'transaction'

    @Shared
    protected TransactionTestContext transactionTestContext

    def setupSpec() {
        // Parent setupSpec() is called automatically for base data
        transactionTestContext = testFixtures.createTransactionContext(testOwner)
    }

    void 'should successfully insert new transaction with isolated test data'() {
        given:
        Transaction transaction = SmartTransactionBuilder.builderForOwner(testOwner)
                .withUniqueGuid()
                .withUniqueDescription("newtxn")
                .withAmount("42.50")
                .buildAndValidate()

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, transaction.toString())

        then:
        response.statusCode == HttpStatus.CREATED
        response.body.contains(transaction.description)
        0 * _
    }

    void 'should reject duplicate transaction insertion'() {
        given:
        Transaction transaction = transactionTestContext.createUniqueTransaction("duplicate")

        // Insert first time
        ResponseEntity<String> firstInsert = insertEndpoint(endpointName, transaction.toString())

        when:
        // Try to insert same transaction again (same GUID)
        ResponseEntity<String> response = insertEndpoint(endpointName, transaction.toString())

        then:
        firstInsert.statusCode == HttpStatus.CREATED
        response.statusCode == HttpStatus.CONFLICT
        0 * _
    }

    void 'should successfully find transaction by guid'() {
        given:
        Transaction transaction = transactionTestContext.createUniqueTransaction("findable")
        insertEndpoint(endpointName, transaction.toString())

        when:
        ResponseEntity<String> response = selectEndpoint(endpointName, transaction.guid)

        then:
        response.statusCode == HttpStatus.OK
        response.body.contains(transaction.guid)
        0 * _
    }

    void 'should return not found for non-existent transaction guid'() {
        when:
        ResponseEntity<String> response = selectEndpoint(endpointName, UUID.randomUUID().toString())

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'should successfully delete transaction by guid'() {
        given:
        Transaction transaction = transactionTestContext.createUniqueTransaction("todelete")
        ResponseEntity<String> insertResponse = insertEndpoint(endpointName, transaction.toString())

        when:
        ResponseEntity<String> response = deleteEndpoint(endpointName, transaction.guid)

        then:
        insertResponse.statusCode == HttpStatus.CREATED
        response.statusCode == HttpStatus.OK
        0 * _
    }

    void 'should reject transaction insertion with empty description'() {
        when:
        SmartTransactionBuilder.builderForOwner(testOwner)
                .withDescription('')
                .buildAndValidate()

        then:
        thrown(IllegalStateException) // Validation should catch this during buildAndValidate()
        0 * _
    }

    void 'should reject transaction insertion with invalid guid format'() {
        given:
        Transaction invalidTransaction = SmartTransactionBuilder.builderForOwner(testOwner)
                .withGuid("badGuid")  // Invalid GUID format - not UUID
                .build()  // Use build() not buildAndValidate() to allow invalid data

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, invalidTransaction.toString())

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'should reject transaction insertion with missing required fields'() {
        given:
        // Create JSON with missing required fields (guid, transactionType, accountNameOwner, category, transactionState)
        String invalidPayload = """
        {
            "accountId": 0,
            "accountType": "credit",
            "transactionDate": "2020-10-05",
            "description": "test transaction",
            "amount": 3.14
        }
        """

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, invalidPayload)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'should reject transaction insertion with invalid category length'() {
        given:
        String longCategory = "a" * 51  // Exceeds 50 char limit
        Transaction invalidTransaction = SmartTransactionBuilder.builderForOwner(testOwner)
                .withCategory(longCategory)  // Invalid category length
                .build()  // Use build() not buildAndValidate() to allow invalid data

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, invalidTransaction.toString())

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'should successfully handle different transaction types'() {
        given:
        Transaction expenseTransaction = transactionTestContext.createExpenseTransaction("expense_test", new BigDecimal("100.00"))
        Transaction incomeTransaction = transactionTestContext.createIncomeTransaction("income_test", new BigDecimal("500.00"))

        when:
        ResponseEntity<String> expenseResponse = insertEndpoint(endpointName, expenseTransaction.toString())
        ResponseEntity<String> incomeResponse = insertEndpoint(endpointName, incomeTransaction.toString())

        then:
        expenseResponse.statusCode == HttpStatus.CREATED
        incomeResponse.statusCode == HttpStatus.CREATED
        expenseResponse.body.contains("expense_test")
        incomeResponse.body.contains("income_test")
        0 * _
    }

    void 'should successfully handle different account types'() {
        given:
        // Use SmartTransactionBuilder to generate constraint-compliant account names
        Transaction creditTransaction = SmartTransactionBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("credit")
                .withDescription("credit_test")
                .asCredit()
                .buildAndValidate()
        Transaction debitTransaction = SmartTransactionBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("debit")
                .withDescription("debit_test")
                .asDebit()
                .buildAndValidate()

        when:
        ResponseEntity<String> creditResponse = insertEndpoint(endpointName, creditTransaction.toString())
        ResponseEntity<String> debitResponse = insertEndpoint(endpointName, debitTransaction.toString())

        then:
        creditResponse.statusCode == HttpStatus.CREATED
        debitResponse.statusCode == HttpStatus.CREATED
        creditResponse.body.contains("credit_test")
        debitResponse.body.contains("debit_test")
        0 * _
    }

    void 'should successfully handle different transaction states'() {
        given:
        Transaction clearedTransaction = transactionTestContext.createClearedTransaction("cleared_test")
        Transaction futureTransaction = transactionTestContext.createFutureTransaction("future_test")
        Transaction outstandingTransaction = transactionTestContext.createOutstandingTransaction("outstanding_test")

        when:
        ResponseEntity<String> clearedResponse = insertEndpoint(endpointName, clearedTransaction.toString())
        ResponseEntity<String> futureResponse = insertEndpoint(endpointName, futureTransaction.toString())
        ResponseEntity<String> outstandingResponse = insertEndpoint(endpointName, outstandingTransaction.toString())

        then:
        clearedResponse.statusCode == HttpStatus.CREATED
        futureResponse.statusCode == HttpStatus.CREATED
        outstandingResponse.statusCode == HttpStatus.CREATED
        clearedResponse.body.contains("cleared_test")
        futureResponse.body.contains("future_test")
        outstandingResponse.body.contains("outstanding_test")
        0 * _
    }

    void 'should successfully handle business category transactions'() {
        given:
        Transaction onlineTransaction = transactionTestContext.createOnlineTransaction()
        Transaction groceryTransaction = transactionTestContext.createGroceryTransaction()
        Transaction utilityTransaction = transactionTestContext.createUtilityTransaction()
        Transaction restaurantTransaction = transactionTestContext.createRestaurantTransaction()

        when:
        ResponseEntity<String> onlineResponse = insertEndpoint(endpointName, onlineTransaction.toString())
        ResponseEntity<String> groceryResponse = insertEndpoint(endpointName, groceryTransaction.toString())
        ResponseEntity<String> utilityResponse = insertEndpoint(endpointName, utilityTransaction.toString())
        ResponseEntity<String> restaurantResponse = insertEndpoint(endpointName, restaurantTransaction.toString())

        then:
        onlineResponse.statusCode == HttpStatus.CREATED
        groceryResponse.statusCode == HttpStatus.CREATED
        utilityResponse.statusCode == HttpStatus.CREATED
        restaurantResponse.statusCode == HttpStatus.CREATED

        onlineResponse.body.contains("online")
        groceryResponse.body.contains("groceries")
        utilityResponse.body.contains("utilities")
        restaurantResponse.body.contains("dining")
        0 * _
    }

    void 'should successfully handle active and inactive transactions'() {
        given:
        Transaction activeTransaction = transactionTestContext.createActiveTransaction("active_transaction")
        Transaction inactiveTransaction = transactionTestContext.createInactiveTransaction("inactive_transaction")

        when:
        ResponseEntity<String> activeResponse = insertEndpoint(endpointName, activeTransaction.toString())
        ResponseEntity<String> inactiveResponse = insertEndpoint(endpointName, inactiveTransaction.toString())

        then:
        activeResponse.statusCode == HttpStatus.CREATED
        inactiveResponse.statusCode == HttpStatus.CREATED
        activeResponse.body.contains('"activeStatus":true')
        inactiveResponse.body.contains('"activeStatus":false')
        0 * _
    }

    void 'should reject transaction with invalid amount format'() {
        given:
        // For invalid amount format, need to use raw JSON since SmartTransactionBuilder only accepts BigDecimal
        String invalidPayload = """
        {
            "guid": "${UUID.randomUUID()}",
            "accountId": 0,
            "accountType": "credit",
            "transactionType": "expense",
            "accountNameOwner": "account_${testOwner.replaceAll(/[^a-z]/, '').toLowerCase()}",
            "transactionDate": "2020-10-05",
            "description": "test transaction",
            "category": "online${testOwner.replaceAll(/[^a-z0-9]/, '').toLowerCase()}",
            "amount": "invalid_amount",
            "transactionState": "cleared",
            "reoccurringType": "undefined",
            "activeStatus": true,
            "notes": ""
        }
        """

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, invalidPayload)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'should reject transaction with invalid JSON format'() {
        given:
        String malformedPayload = '{badJson:"test"}'
        String badJson = 'badJson'

        when:
        ResponseEntity<String> response1 = insertEndpoint(endpointName, malformedPayload)
        ResponseEntity<String> response2 = insertEndpoint(endpointName, badJson)

        then:
        response1.statusCode == HttpStatus.BAD_REQUEST
        response2.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'should return not found when deleting non-existent transaction'() {
        when:
        ResponseEntity<String> response = deleteEndpoint(endpointName, UUID.randomUUID().toString())

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'should complete full transaction lifecycle - insert, find, delete'() {
        given:
        Transaction transaction = transactionTestContext.createUniqueTransaction("lifecycle")

        when: 'insert transaction'
        ResponseEntity<String> insertResponse = insertEndpoint(endpointName, transaction.toString())

        then: 'insert succeeds'
        insertResponse.statusCode == HttpStatus.CREATED
        insertResponse.body.contains(transaction.description)
        0 * _

        when: 'find transaction'
        ResponseEntity<String> findResponse = selectEndpoint(endpointName, transaction.guid)

        then: 'find succeeds'
        findResponse.statusCode == HttpStatus.OK
        findResponse.body.contains(transaction.guid)
        0 * _

        when: 'attempt duplicate insert'
        ResponseEntity<String> duplicateResponse = insertEndpoint(endpointName, transaction.toString())

        then: 'duplicate is rejected'
        duplicateResponse.statusCode == HttpStatus.CONFLICT
        0 * _

        when: 'delete transaction'
        ResponseEntity<String> deleteResponse = deleteEndpoint(endpointName, transaction.guid)

        then: 'delete succeeds'
        deleteResponse.statusCode == HttpStatus.OK
        0 * _

        when: 'find after delete'
        ResponseEntity<String> findAfterDeleteResponse = selectEndpoint(endpointName, transaction.guid)

        then: 'transaction not found after delete'
        findAfterDeleteResponse.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'should handle constraint validation for description length'() {
        given:
        String descriptionTooLong = "a" * 76  // Exceeds 75 char limit

        when:
        SmartTransactionBuilder.builderForOwner(testOwner)
                .withDescription(descriptionTooLong)
                .buildAndValidate()

        then:
        thrown(IllegalStateException)
        0 * _
    }

    void 'should handle constraint validation for account name owner length'() {
        given:
        String accountNameTooLong = "a" * 41  // Exceeds 40 char limit

        when:
        SmartTransactionBuilder.builderForOwner(testOwner)
                .withAccountNameOwner(accountNameTooLong)
                .buildAndValidate()

        then:
        thrown(IllegalStateException)
        0 * _
    }

    void 'should handle constraint validation for notes length'() {
        given:
        String notesTooLong = "a" * 101  // Exceeds 100 char limit

        when:
        SmartTransactionBuilder.builderForOwner(testOwner)
                .withNotes(notesTooLong)
                .buildAndValidate()

        then:
        thrown(IllegalStateException)
        0 * _
    }

    void 'should fail to update transaction receipt image with invalid data'() {
        given:
        String jpegImage = 'data:image/jpeg;base64,/9j/2wBDAAMCAgICAgMCAgIDAwMDBAYEBAQEBAgGBgUGCQgKCgkICQkKDA=MCgsOCwkJDRENDg8QEBEQCgwSExIQEw8QEBD/yQALCAABAAEBAREA/8wABgAQEAX/2gAIAQEAAD8A0s8g/9k='
        String guid = UUID.randomUUID().toString()
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(jpegImage, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/api/transaction/update/receipt/image/${guid}"), HttpMethod.PUT,
                entity, String)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        0 * _
    }
}