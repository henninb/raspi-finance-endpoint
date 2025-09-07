package finance.controllers

import finance.domain.Payment
import finance.domain.Account
import finance.helpers.SmartPaymentBuilder
import finance.helpers.SmartAccountBuilder
import finance.helpers.PaymentTestContext
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared
import java.sql.Date

@ActiveProfiles("func")
class PaymentControllerIsolatedSpec extends BaseControllerSpec {

    @Shared
    protected String endpointName = 'payment'

    @Shared
    protected PaymentTestContext paymentTestContext

    def setupSpec() {
        // Parent setupSpec() is called automatically for base data
        paymentTestContext = testFixtures.createPaymentContext(testOwner)
    }

    void 'should successfully insert new payment with isolated test data'() {
        given:
        // Create accounts using SmartAccountBuilder and HTTP endpoints like other working tests
        Account sourceAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("paysrc")
                .asDebit()
                .buildAndValidate()
        Account destAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("paydest")
                .asCredit()
                .buildAndValidate()

        ResponseEntity<String> sourceAccountResponse = insertEndpoint('account', sourceAccount.toString())
        ResponseEntity<String> destAccountResponse = insertEndpoint('account', destAccount.toString())

        // Create payment using SmartPaymentBuilder
        Payment payment = SmartPaymentBuilder.builderForOwner(testOwner)
                .withSourceAccount(sourceAccount.accountNameOwner)
                .withDestinationAccount(destAccount.accountNameOwner)
                .withAmount(25.00G)
                .withTransactionDate(Date.valueOf('2023-01-01'))
                .asActive()
                .buildAndValidate()

        when:
        ResponseEntity<String> response = insertEndpoint(endpointName, payment.toString())

        then:
        sourceAccountResponse.statusCode == HttpStatus.CREATED
        destAccountResponse.statusCode == HttpStatus.CREATED
        response.statusCode == HttpStatus.CREATED
        response.body.contains('"sourceAccount":"' + sourceAccount.accountNameOwner + '"')
        response.body.contains('"destinationAccount":"' + destAccount.accountNameOwner + '"')
        response.body.contains('"amount":25.0')
        0 * _
    }

    void 'should successfully handle different payment amounts'() {
        given:
        // Create accounts using SmartAccountBuilder and HTTP endpoints
        Account sourceAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("amountsrc")
                .asDebit()
                .buildAndValidate()
        Account destAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("amountdest")
                .asCredit()
                .buildAndValidate()

        ResponseEntity<String> sourceAccountResponse = insertEndpoint('account', sourceAccount.toString())
        ResponseEntity<String> destAccountResponse = insertEndpoint('account', destAccount.toString())

        // Create small payment using SmartPaymentBuilder
        Payment paymentSmall = SmartPaymentBuilder.builderForOwner(testOwner)
                .withSourceAccount(sourceAccount.accountNameOwner)
                .withDestinationAccount(destAccount.accountNameOwner)
                .withAmount(5.00G)
                .withTransactionDate(Date.valueOf('2023-01-01'))
                .asActive()
                .buildAndValidate()

        // Create large payment using SmartPaymentBuilder
        Payment paymentLarge = SmartPaymentBuilder.builderForOwner(testOwner)
                .withSourceAccount(sourceAccount.accountNameOwner)
                .withDestinationAccount(destAccount.accountNameOwner)
                .withAmount(999.99G)
                .withTransactionDate(Date.valueOf('2023-01-02'))
                .asActive()
                .buildAndValidate()

        when:
        ResponseEntity<String> smallResponse = insertEndpoint(endpointName, paymentSmall.toString())
        ResponseEntity<String> largeResponse = insertEndpoint(endpointName, paymentLarge.toString())

        then:
        sourceAccountResponse.statusCode == HttpStatus.CREATED
        destAccountResponse.statusCode == HttpStatus.CREATED
        smallResponse.statusCode == HttpStatus.CREATED
        smallResponse.body.contains('"amount":5.0')
        largeResponse.statusCode == HttpStatus.CREATED
        largeResponse.body.contains('"amount":999.99')
        0 * _
    }

    void 'should successfully handle active and inactive payments'() {
        given:
        // Create accounts using SmartAccountBuilder and HTTP endpoints
        Account sourceAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("statussrc")
                .asDebit()
                .buildAndValidate()
        Account destAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("statusdest")
                .asCredit()
                .buildAndValidate()

        ResponseEntity<String> sourceAccountResponse = insertEndpoint('account', sourceAccount.toString())
        ResponseEntity<String> destAccountResponse = insertEndpoint('account', destAccount.toString())

        // Create active payment using SmartPaymentBuilder
        Payment activePayment = SmartPaymentBuilder.builderForOwner(testOwner)
                .withSourceAccount(sourceAccount.accountNameOwner)
                .withDestinationAccount(destAccount.accountNameOwner)
                .withAmount(150.00G)
                .withTransactionDate(Date.valueOf('2023-01-03'))
                .asActive()
                .buildAndValidate()

        // Create inactive payment using SmartPaymentBuilder
        Payment inactivePayment = SmartPaymentBuilder.builderForOwner(testOwner)
                .withSourceAccount(sourceAccount.accountNameOwner)
                .withDestinationAccount(destAccount.accountNameOwner)
                .withAmount(250.00G)
                .withTransactionDate(Date.valueOf('2023-01-04'))
                .asInactive()
                .buildAndValidate()

        when:
        ResponseEntity<String> activeResponse = insertEndpoint(endpointName, activePayment.toString())
        ResponseEntity<String> inactiveResponse = insertEndpoint(endpointName, inactivePayment.toString())

        then:
        sourceAccountResponse.statusCode == HttpStatus.CREATED
        destAccountResponse.statusCode == HttpStatus.CREATED
        activeResponse.statusCode == HttpStatus.CREATED
        activeResponse.body.contains('"activeStatus":true')
        inactiveResponse.statusCode == HttpStatus.CREATED
        inactiveResponse.body.contains('"activeStatus":false')
        0 * _
    }

    void 'should successfully select all payments'() {
        given:
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/api/payment/select"),
                HttpMethod.GET, entity, String)

        then:
        response.statusCode == HttpStatus.OK
        response.body.startsWith('[') // Should be a JSON array
        0 * _
    }

    void 'should reject payment with invalid JSON payload'() {
        given:
        String invalidJson = '{"invalid": "data"}'
        String malformedJson = 'not valid json'

        when:
        ResponseEntity<String> response1 = insertEndpoint(endpointName, invalidJson)
        ResponseEntity<String> response2 = insertEndpoint(endpointName, malformedJson)

        then:
        response1.statusCode == HttpStatus.BAD_REQUEST
        response2.statusCode == HttpStatus.BAD_REQUEST
        0 * _
    }

    void 'should reject duplicate payment insertion with conflict status'() {
        given:
        // Create accounts using SmartAccountBuilder and HTTP endpoints
        Account sourceAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("dupsrc")
                .asDebit()
                .buildAndValidate()
        Account destAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("dupdest")
                .asCredit()
                .buildAndValidate()

        ResponseEntity<String> sourceAccountResponse = insertEndpoint('account', sourceAccount.toString())
        ResponseEntity<String> destAccountResponse = insertEndpoint('account', destAccount.toString())

        // Create payment with specific GUIDs for duplication testing
        Payment payment = SmartPaymentBuilder.builderForOwner(testOwner)
                .withSourceAccount(sourceAccount.accountNameOwner)
                .withDestinationAccount(destAccount.accountNameOwner)
                .withAmount(123.45G)
                .withTransactionDate(Date.valueOf('2023-12-01'))
                .withGuidSource('ba665bc2-22b6-4123-a566-6f5ab3d796d3')
                .withGuidDestination('ba665bc2-22b6-4123-a566-6f5ab3d796d4')
                .asActive()
                .buildAndValidate()

        when:
        ResponseEntity<String> firstResponse = insertEndpoint(endpointName, payment.toString())
        ResponseEntity<String> duplicateResponse = insertEndpoint(endpointName, payment.toString())

        then:
        sourceAccountResponse.statusCode == HttpStatus.CREATED
        destAccountResponse.statusCode == HttpStatus.CREATED
        firstResponse.statusCode == HttpStatus.CREATED
        duplicateResponse.statusCode == HttpStatus.CONFLICT
        0 * _
    }

    void 'should successfully delete payment by ID'() {
        given:
        // Create accounts using SmartAccountBuilder and HTTP endpoints
        Account sourceAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("delsrc")
                .asDebit()
                .buildAndValidate()
        Account destAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("deldest")
                .asCredit()
                .buildAndValidate()

        ResponseEntity<String> sourceAccountResponse = insertEndpoint('account', sourceAccount.toString())
        ResponseEntity<String> destAccountResponse = insertEndpoint('account', destAccount.toString())

        // Create payment using SmartBuilder
        Payment payment = SmartPaymentBuilder.builderForOwner(testOwner)
                .withSourceAccount(sourceAccount.accountNameOwner)
                .withDestinationAccount(destAccount.accountNameOwner)
                .withAmount(88.88G)
                .withTransactionDate(Date.valueOf('2023-01-01'))
                .asActive()
                .buildAndValidate()

        // Insert payment and extract ID
        ResponseEntity<String> insertResponse = insertEndpoint(endpointName, payment.toString())
        assert sourceAccountResponse.statusCode == HttpStatus.CREATED
        assert destAccountResponse.statusCode == HttpStatus.CREATED
        assert insertResponse.statusCode == HttpStatus.CREATED

        String paymentIdStr = (insertResponse.body =~ /"paymentId":(\d+)/)[0][1]
        Long paymentId = Long.parseLong(paymentIdStr)

        when:
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                createURLWithPort("/api/payment/delete/${paymentId}"),
                HttpMethod.DELETE, entity, String)

        then:
        deleteResponse.statusCode == HttpStatus.OK
        deleteResponse.body.contains('"paymentId":' + paymentId)
        0 * _
    }

    void 'should return not found when deleting non-existent payment'() {
        given:
        Long nonExistentId = 999999L

        when:
        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/api/payment/delete/${nonExistentId}"),
                HttpMethod.DELETE, entity, String)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        0 * _
    }

    void 'should handle unauthorized access to payment endpoints'() {
        given:
        HttpHeaders cleanHeaders = new HttpHeaders()
        cleanHeaders.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity entity = new HttpEntity<>(null, cleanHeaders)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/api/payment/select"),
                HttpMethod.GET, entity, String)

        then:
        // Unauthenticated requests to protected endpoints should be forbidden
        response.statusCode == HttpStatus.FORBIDDEN
        0 * _
    }
}
