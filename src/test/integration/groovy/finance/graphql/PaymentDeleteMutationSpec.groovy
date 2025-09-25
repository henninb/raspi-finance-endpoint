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

class PaymentDeleteMutationSpec extends BaseIntegrationSpec {

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

    def "deletePayment mutation returns true for existing payment"() {
        given:
        withUserAuthority()
        def createDto = new PaymentInputDto(
                null,
                primaryAccountName,
                secondaryAccountName,
                Date.valueOf("2024-01-15"),
                new BigDecimal("55.00"),
                null
        )
        def created = mutationController.createPayment(createDto)

        when:
        def deleted = mutationController.deletePayment(created.paymentId)

        then:
        deleted == true
    }

    def "deletePayment mutation returns false for missing payment id"() {
        given:
        withUserAuthority()

        expect:
        mutationController.deletePayment(-9999L) == false
    }
}
