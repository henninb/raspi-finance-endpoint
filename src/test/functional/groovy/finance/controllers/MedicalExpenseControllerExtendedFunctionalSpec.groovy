package finance.controllers

import finance.domain.ClaimStatus
import finance.domain.MedicalExpense
import finance.domain.Transaction
import finance.helpers.SmartMedicalExpenseBuilder
import finance.helpers.SmartTransactionBuilder
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("func")
class MedicalExpenseControllerExtendedFunctionalSpec extends BaseControllerFunctionalSpec {

    private static final String ENDPOINT = 'medical-expenses'

    private ResponseEntity<String> postMedicalExpenseForNewTransaction(BigDecimal billed = 350.00G,
                                                                      ClaimStatus claimStatus = ClaimStatus.Submitted) {
        Transaction tx = SmartTransactionBuilder.builderForOwner(testOwner)
                .withUniqueDescription("med-extended-${System.nanoTime()}")
                .buildAndValidate()

        ResponseEntity<String> txResp = insertEndpoint("transaction", tx.toString())
        assert txResp.statusCode == HttpStatus.CREATED
        Long txId = extractLong(txResp.body, 'transactionId')

        MedicalExpense me = SmartMedicalExpenseBuilder.builderForOwner(testOwner)
                .withTransactionId(txId)
                .withBilledAmount(billed)
                .withClaimStatus(claimStatus)
                .buildAndValidate()

        return insertEndpoint(ENDPOINT, me.toString())
    }

    void 'should retrieve medical expense by transaction id'() {
        when:
        def postResp = postMedicalExpenseForNewTransaction()
        Long txId = extractLong(postResp.body, 'transactionId')
        def getResp = restTemplate.exchange(
                createURLWithPort("/api/${ENDPOINT}/transaction/${txId}"),
                HttpMethod.GET, new HttpEntity<>(null, headers), String)

        then:
        postResp.statusCode == HttpStatus.CREATED
        getResp.statusCode == HttpStatus.OK
        extractLong(getResp.body, 'transactionId') == txId
    }

    void 'should update claim status via patch'() {
        when:
        def postResp = postMedicalExpenseForNewTransaction(200.00G, ClaimStatus.Submitted)
        Long meId = extractLong(postResp.body, 'medicalExpenseId')

        headers.setContentType(MediaType.APPLICATION_JSON)
        String token = generateJwtToken(username)
        headers.set("Cookie", "token=${token}")
        HttpEntity entity = new HttpEntity<>(null, headers)

        ResponseEntity<String> patchResp
        try {
            patchResp = restTemplate.exchange(
                    createURLWithPort("/api/${ENDPOINT}/${meId}/claim-status?claimStatus=Paid"),
                    HttpMethod.PUT, entity, String)
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            patchResp = new ResponseEntity<>(ex.getResponseBodyAsString(), ex.getResponseHeaders(), ex.getStatusCode())
        }

        then:
        postResp.statusCode == HttpStatus.CREATED
        patchResp.statusCode == HttpStatus.OK
        patchResp.body.contains('Claim status updated successfully')
    }

    void 'should soft delete medical expense'() {
        when:
        def postResp = postMedicalExpenseForNewTransaction()
        Long meId = extractLong(postResp.body, 'medicalExpenseId')
        def delResp = restTemplate.exchange(
                createURLWithPort("/api/${ENDPOINT}/delete/${meId}"),
                HttpMethod.DELETE, new HttpEntity<>(null, headers), String)

        then:
        postResp.statusCode == HttpStatus.CREATED
        delResp.statusCode == HttpStatus.OK
        delResp.body.contains('Medical expense deleted successfully')
    }

    void 'should filter by procedure and diagnosis code'() {
        given:
        def postResp = postMedicalExpenseForNewTransaction()
        String proc = extractString(postResp.body, 'procedureCode')
        String diag = extractString(postResp.body, 'diagnosisCode')

        when:
        def byProc = restTemplate.exchange(
                createURLWithPort("/api/${ENDPOINT}/procedure-code/${proc}"),
                HttpMethod.GET, new HttpEntity<>(null, headers), String)
        def byDiag = restTemplate.exchange(
                createURLWithPort("/api/${ENDPOINT}/diagnosis-code/${diag}"),
                HttpMethod.GET, new HttpEntity<>(null, headers), String)

        then:
        postResp.statusCode == HttpStatus.CREATED
        byProc.statusCode == HttpStatus.OK
        byDiag.statusCode == HttpStatus.OK
        byProc.body.contains(proc)
        byDiag.body.contains(diag)
    }

    void 'should list open claims and outstanding balances'() {
        when:
        // Open claim (not paid/closed/denied and active)
        def openResp = postMedicalExpenseForNewTransaction(300.00G, ClaimStatus.Processing)
        // Outstanding balance implies patientResponsibility > 0 and no paidDate (default meets this)
        def getOpen = restTemplate.exchange(
                createURLWithPort("/api/${ENDPOINT}/open-claims"),
                HttpMethod.GET, new HttpEntity<>(null, headers), String)
        def getOutstanding = restTemplate.exchange(
                createURLWithPort("/api/${ENDPOINT}/outstanding-balances"),
                HttpMethod.GET, new HttpEntity<>(null, headers), String)

        then:
        openResp.statusCode == HttpStatus.CREATED
        getOpen.statusCode == HttpStatus.OK
        getOutstanding.statusCode == HttpStatus.OK
    }

    // Helpers
    private Long extractLong(String json, String field) {
        def m = (json =~ /\"${field}\":(\d+)/)
        return m ? Long.parseLong(m[0][1]) : 0L
    }

    private String extractString(String json, String field) {
        def m = (json =~ /\"${field}\":\"([^\"]+)\"/)
        return m ? m[0][1] : ''
    }
}
