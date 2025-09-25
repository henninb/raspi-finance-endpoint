package finance.graphql

import finance.BaseIntegrationSpec
import finance.controllers.GraphQLMutationController
import finance.controllers.dto.PaymentInputDto
import finance.domain.Account
import finance.helpers.SmartAccountBuilder
import finance.repositories.AccountRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

import java.math.BigDecimal
import java.sql.Date
import jakarta.validation.ConstraintViolationException

class PaymentMutationSpec extends BaseIntegrationSpec {

    @Autowired
    AccountRepository accountRepository

    @Autowired
    GraphQLMutationController mutationController

    def setup() {
        // Ensure source and destination accounts exist once per test run
        def existingSource = accountRepository.findByAccountNameOwner(primaryAccountName)
        if (existingSource.isEmpty()) {
            Account source = SmartAccountBuilder.builderForOwner(testOwner)
                    .withAccountNameOwner(primaryAccountName)
                    .asDebit()
                    .withCleared(new BigDecimal("1000.00"))
                    .buildAndValidate()
            accountRepository.save(source)
        }

        def existingDest = accountRepository.findByAccountNameOwner(secondaryAccountName)
        if (existingDest.isEmpty()) {
            Account dest = SmartAccountBuilder.builderForOwner(testOwner)
                    .withAccountNameOwner(secondaryAccountName)
                    .asCredit()
                    .withCleared(new BigDecimal("-200.00"))
                    .buildAndValidate()
            accountRepository.save(dest)
        }
    }

    private static void withUserAuthority() {
        def auth = new UsernamePasswordAuthenticationToken(
                "test-user",
                "N/A",
                [new SimpleGrantedAuthority("USER")]
        )
        SecurityContextHolder.getContext().setAuthentication(auth)
    }

    def "createPayment mutation succeeds with valid input"() {
        given:
        withUserAuthority()
        def dto = new PaymentInputDto(
                null,
                primaryAccountName,
                secondaryAccountName,
                Date.valueOf("2024-01-15"),
                new BigDecimal("123.45"),
                null
        )

        when:
        def result = mutationController.createPayment(dto)

        then:
        result != null
        result.paymentId > 0
        result.sourceAccount == primaryAccountName
        result.destinationAccount == secondaryAccountName
        result.amount == new BigDecimal("123.45")
        result.transactionDate == Date.valueOf("2024-01-15")
        result.activeStatus == true
    }

    def "createPayment mutation fails validation for negative amount"() {
        given:
        withUserAuthority()
        def dto = new PaymentInputDto(
                null,
                primaryAccountName,
                secondaryAccountName,
                Date.valueOf("2024-01-15"),
                new BigDecimal("-5.00"),
                null
        )

        when:
        mutationController.createPayment(dto)

        then:
        thrown(ConstraintViolationException)
    }
}
