package finance.controllers

import finance.domain.*
import finance.helpers.SmartTransactionBuilder
import finance.helpers.SmartMedicalExpenseBuilder
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared
import java.math.BigDecimal
import java.sql.Date

@ActiveProfiles("func")
class MedicalExpenseControllerIsolatedSpec extends BaseControllerSpec {

    @Shared
    protected String endpointName = 'medical-expenses'

    def setupSpec() {
        // Parent setupSpec() is called automatically
    }

    void 'should successfully insert new medical expense with isolated test data'() {
        given: 'a transaction and medical expense linked to test owner'
        // Create a test transaction first
        Transaction transaction = SmartTransactionBuilder.builderForOwner(testOwner)
                .withUniqueDescription("medical-test")
                .buildAndValidate()
        
        // Insert the transaction to get a valid transaction ID
        ResponseEntity<String> transactionResponse = insertEndpoint("transaction", transaction.toString())
        
        // Create medical expense linked to the transaction
        MedicalExpense medicalExpense = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(extractTransactionIdFromResponse(transactionResponse.body))
                .buildAndValidate()

        when: 'posting to medical expenses endpoint'
        ResponseEntity<String> response = insertEndpoint(endpointName, medicalExpense.toString())

        then: 'should return created status and echo fields'
        transactionResponse.statusCode == HttpStatus.CREATED
        response.statusCode == HttpStatus.CREATED
        // Parse returned JSON and verify key fields
        def created = new groovy.json.JsonSlurper().parseText(response.body)
        (created.transactionId as Long) == medicalExpense.transactionId
        (created.billedAmount as BigDecimal) == medicalExpense.billedAmount
        created.claimStatus == medicalExpense.claimStatus.toString().toLowerCase()
        0 * _
    }

    void 'should reject duplicate medical expense for same transaction'() {
        given: 'a transaction and two medical expenses for same transaction'
        Transaction transaction = SmartTransactionBuilder.builderForOwner(testOwner)
                .withUniqueDescription("duplicate-medical")
                .buildAndValidate()
        
        ResponseEntity<String> transactionResponse = insertEndpoint("transaction", transaction.toString())
        Long transactionId = extractTransactionIdFromResponse(transactionResponse.body)
        
        // First medical expense
        MedicalExpense firstMedicalExpense = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId)
                .buildAndValidate()
        
        // Second medical expense for same transaction
        MedicalExpense secondMedicalExpense = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId)
                .buildAndValidate()

        ResponseEntity<String> firstResponse = insertEndpoint(endpointName, firstMedicalExpense.toString())

        when: 'attempting to insert second medical expense for same transaction'
        ResponseEntity<String> response = insertEndpoint(endpointName, secondMedicalExpense.toString())

        then: 'first insert succeeds, second is rejected with 409 Conflict'
        transactionResponse.statusCode == HttpStatus.CREATED
        firstResponse.statusCode == HttpStatus.CREATED
        response.statusCode == HttpStatus.CONFLICT
        0 * _
    }

    void 'should reject medical expense with invalid financial amounts'() {
        given: 'a transaction and medical expense with invalid financial validation'
        Transaction transaction = SmartTransactionBuilder.builderForOwner(testOwner)
                .withUniqueDescription("invalid-financial")
                .buildAndValidate()
        
        ResponseEntity<String> transactionResponse = insertEndpoint("transaction", transaction.toString())
        Long transactionId = extractTransactionIdFromResponse(transactionResponse.body)
        
        // Create medical expense with invalid amounts (billed < allocated)
        MedicalExpense invalidMedicalExpense = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(transactionId)
                .withBilledAmount(new BigDecimal("100.00"))
                .withInsurancePaid(new BigDecimal("75.00"))
                .withPatientResponsibility(new BigDecimal("50.00")) // Total exceeds billed
                .build() // Use build() instead of buildAndValidate() to allow invalid data

        when: 'attempting to insert invalid medical expense'
        ResponseEntity<String> response = insertEndpoint(endpointName, invalidMedicalExpense.toString())

        then: 'should be rejected at server with 500'
        transactionResponse.statusCode == HttpStatus.CREATED
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        0 * _
    }

    // Helper method to extract transaction ID from response
    private Long extractTransactionIdFromResponse(String responseBody) {
        // Parse JSON response to extract transaction ID
        // This is a simplified implementation - in real tests this would use proper JSON parsing
        def matcher = responseBody =~ /"transactionId":(\d+)/
        return matcher ? Long.parseLong(matcher[0][1]) : 1001L
    }
}
