package finance.graphql

import finance.BaseIntegrationSpec
import finance.controllers.GraphQLMutationController
import finance.controllers.dto.PaymentInputDto
import finance.domain.Account
import finance.domain.Transfer
import finance.helpers.SmartAccountBuilder
import finance.repositories.AccountRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

import java.math.BigDecimal
import java.sql.Date
import jakarta.validation.ConstraintViolationException

class TransferMutationSpec extends BaseIntegrationSpec {

    @Autowired
    AccountRepository accountRepository

    @Autowired
    GraphQLMutationController mutationController

    def setup() {
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

    def "createTransfer mutation succeeds with valid input"() {
        given:
        withUserAuthority()

        when:
        def result = mutationController.createTransfer(
                new finance.controllers.dto.TransferInputDto(
                        null,
                        primaryAccountName,
                        secondaryAccountName,
                        Date.valueOf("2024-02-01"),
                        new BigDecimal("300.00"),
                        null
                )
        )

        then:
        result != null
        result.transferId > 0
        result.sourceAccount == primaryAccountName
        result.destinationAccount == secondaryAccountName
        result.amount == new BigDecimal("300.00")
        result.transactionDate == Date.valueOf("2024-02-01")
        result.activeStatus == true
    }

    def "createTransfer mutation fails validation for negative amount"() {
        given:
        withUserAuthority()

        when:
        mutationController.createTransfer(
                new finance.controllers.dto.TransferInputDto(
                        null,
                        primaryAccountName,
                        secondaryAccountName,
                        Date.valueOf("2024-02-01"),
                        new BigDecimal("-1.00"),
                        null
                )
        )

        then:
        thrown(ConstraintViolationException)
    }

    def "deleteTransfer mutation returns true for existing transfer"() {
        given:
        withUserAuthority()
        def created = mutationController.createTransfer(
                new finance.controllers.dto.TransferInputDto(
                        null,
                        primaryAccountName,
                        secondaryAccountName,
                        Date.valueOf("2024-02-02"),
                        new BigDecimal("25.00"),
                        null
                )
        )

        when:
        def deleted = mutationController.deleteTransfer(created.transferId)

        then:
        deleted == true
    }

    def "deleteTransfer mutation returns false for missing id"() {
        given:
        withUserAuthority()

        expect:
        mutationController.deleteTransfer(-9999L) == false
    }
}
