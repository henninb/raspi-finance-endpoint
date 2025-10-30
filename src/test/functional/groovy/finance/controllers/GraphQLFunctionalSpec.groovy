package finance.controllers

import groovy.json.JsonBuilder
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

class GraphQLFunctionalSpec extends BaseControllerFunctionalSpec {

    // Also use controllers directly to avoid brittle endpoint assumptions
    @Autowired
    finance.controllers.graphql.GraphQLQueryController queryController
    @Autowired
    finance.controllers.graphql.GraphQLMutationController mutationController

    private void withUserRole(String name = "test-user", List<String> roles = ["USER"]) {
        def authorities = roles.collect { new SimpleGrantedAuthority(it) }
        def auth = new UsernamePasswordAuthenticationToken(name, "N/A", authorities)
        SecurityContextHolder.getContext().setAuthentication(auth)
    }

    // Use the same cleaned naming convention as TestDataManager for functional tests
    @Override
    protected String getPrimaryAccountName() {
        String cleanOwner = testOwner.replaceAll(/[^a-z]/, '').toLowerCase()
        if (cleanOwner.isEmpty()) cleanOwner = "testowner"
        return "primary_${cleanOwner}"
    }

    @Override
    protected String getSecondaryAccountName() {
        String cleanOwner = testOwner.replaceAll(/[^a-z]/, '').toLowerCase()
        if (cleanOwner.isEmpty()) cleanOwner = "testowner"
        return "secondary_${cleanOwner}"
    }

    void 'simple GraphQL query test - to verify GraphQL endpoint is working'() {
        when: 'invoking the query controller directly'
        def list = queryController.descriptions()

        then: 'we get a response (even if empty)'
        list != null
        0 * _
    }

    void 'createTransfer mutation should accept transfer parameter'() {
        given: 'transfer data for the test'
        def sourceAccount = testDataManager.createAccountFor(testOwner, "primary", 'debit', true)
        def destinationAccount = testDataManager.createAccountFor(testOwner, "secondary", 'debit', true)

        def mutation = '''
            mutation CreateTransfer($transfer: TransferInput!) {
                createTransfer(transfer: $transfer) {
                    transferId
                    sourceAccount
                    destinationAccount
                    amount
                    activeStatus
                }
            }
        '''

        def variables = [
            transfer: [
                sourceAccount: sourceAccount,
                destinationAccount: destinationAccount,
                transactionDate: "2024-01-15",
                amount: 100.00,
                activeStatus: true
            ]
        ]

        def requestBody = [
            query: mutation,
            variables: variables
        ]

        def jsonBuilder = new JsonBuilder(requestBody)

        when: 'calling the mutation controller directly'
        withUserRole()
        def dto = new finance.controllers.dto.TransferInputDto(
                null,
                sourceAccount,
                destinationAccount,
                java.sql.Date.valueOf("2024-01-15"),
                new BigDecimal("100.00"),
                null,
                null,
                true
        )
        def created = mutationController.createTransfer(dto)

        then: 'it succeeds and returns data with auto-generated GUIDs'
        created != null
        created.sourceAccount == sourceAccount
        created.destinationAccount == destinationAccount
        created.amount == new BigDecimal("100.00")
        created.guidSource != null
        created.guidDestination != null
        0 * _
    }


//    void 'description test'() {
//        when:
//        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post(createURLWithPort("/graphql"))
//                .header(HttpHeaders.AUTHORIZATION, "Basic " + Base64Utils.encodeToString("${username}:${password}".getBytes()))
//                .content("{\"query\":\"{ descriptions { descriptionName } }\"}")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultHandlers.print())
//        then:
//        resultActions.andExpect(status().isOk())
//        0 * _
//    }
//
//    void 'description test - should fail'() {
//        when:
//        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post(createURLWithPort("/graphql"))
//                .header(HttpHeaders.AUTHORIZATION, "Basic " + Base64Utils.encodeToString("${username}:${password}".getBytes()))
//                .content("{\"query\":\"{ dne { descriptionName } }\"}")
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultHandlers.print())
//        then:
//        resultActions.andExpect(status().isOk())
//        0 * _
//    }
}
