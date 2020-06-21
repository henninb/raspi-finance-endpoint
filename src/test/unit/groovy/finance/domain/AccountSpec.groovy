package finance.domain

import com.fasterxml.jackson.databind.ObjectMapper
import finance.helpers.AccountBuilder
import spock.lang.Specification
import spock.lang.Unroll

import javax.validation.ConstraintViolation
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory

class AccountSpec extends Specification {

    ValidatorFactory validatorFactory
    Validator validator
    private ObjectMapper mapper = new ObjectMapper()

    void setup() {
        validatorFactory = Validation.buildDefaultValidatorFactory()
        validator = validatorFactory.getValidator()
    }

    void cleanup() {
        validatorFactory.close()
    }

    def "test JSON serialization to Account object"() {

        given:
        def jsonPayload = "{\"accountId\":1001,\"accountNameOwner\":\"discover_brian\",\"accountType\":\"credit\",\"activeStatus\":true,\"moniker\":\"1234\",\"totals\":0.01,\"totalsBalanced\":0.02,\"dateClosed\":0,\"dateUpdated\":1553645394000,\"dateAdded\":1553645394000}"

        when:
        Account account = mapper.readValue(jsonPayload, Account.class)

        then:
        account.accountType == AccountType.Credit
        account.accountNameOwner == "discover_brian"
        account.accountId == 1001
    }

    def "test validation valid account"() {
        given:
        Account account = AccountBuilder.builder().build()
        //def json = mapper.writeValueAsString(account)
        //println json

        when:
        Set<ConstraintViolation<Account>> violations = validator.validate(account)

        then:
        violations.isEmpty()
    }


    @Unroll
    def "test validation invalid #invalidField has error #expectedError"() {
        given:
        Account account = new AccountBuilder()
        //.transactionId(transactionId)
                //.guid(guid)
                //.accountId(accountId)
                .accountType(accountType)
                .accountNameOwner(accountNameOwner)
                //.transactionDate(transactionDate)
                //.description(description)
                //.category(category)
                //.amount(amount)
                //.cleared(cleared)
                //.reoccurring(reoccurring)
                //.notes(notes)
                //.dateAdded(dateAdded)
                //.dateUpdated(dateUpdated)
                //.sha256(sha256)
                .build()

        when:
        Set<ConstraintViolation<Account>> violations = validator.validate(account)

        then:
        println("'" + violations.message + "'")
        println("'" + violations.size() + "'")

        violations.size() == errorCount
        violations.message.contains(expectedError)
        violations.iterator().next().getInvalidValue() == account.getProperties()[invalidField]

        where:
        invalidField |  accountType | accountNameOwner | moniker |expectedError | errorCount
        //'accountType'  |  AccountType.Undefined | 'blah.chase_brian' | '0000' |'must be greater than or equal to 0'                   | 1
        'accountNameOwner'  |  AccountType.Credit | 'blah_chase_brian' | '0000' |'must be alpha separated by an underscore'                   | 1
    }
}
