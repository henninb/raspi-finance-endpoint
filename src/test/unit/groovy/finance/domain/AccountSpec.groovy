package finance.domain

import com.fasterxml.jackson.databind.ObjectMapper
import finance.helpers.AccountBuilder
import spock.lang.Specification
import spock.lang.Unroll

import javax.validation.ConstraintViolation
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory
import java.sql.Date
import java.sql.Timestamp

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
                .accountType(accountType)
                .accountNameOwner(accountNameOwner)
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
        //'accountType'  |  AccountType.Undefined | 'chase_brian' | '0000' |''                   | 0
        'accountNameOwner'  |  AccountType.Credit | 'blah_chase_brian' | '0000' |'must be alpha separated by an underscore'                   | 1
        //'moniker'  |  AccountType.Credit | 'chase_brian' | '00034333' |                   'size must be between 4 and 4'| 1
        //'amount'   |  AccountType.Credit | 'blah_chase_brian' | '0000'         | new BigDecimal(3.1415)                                   | 1       | false       | 'no notes' | new Timestamp(1553645394000) | new Timestamp(1553645394000) | 'sha256' | 'must be dollar precision'                 | 1


    }
}
