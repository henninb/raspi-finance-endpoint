package finance.domain

class ValidationAmountSpec extends BaseDomainSpec {

    protected String jsonPayload = '{"accountNameOwner":"chase_brian", "amount":1.23, "activeStatus":true, "transactionState":"cleared"}'

    void 'test -- JSON serialization to ValidationAmount'() {

        when:
        ValidationAmount validationAmount = mapper.readValue(jsonPayload, ValidationAmount)

        then:
        validationAmount.amount == 1.23
        0 * _
    }
}