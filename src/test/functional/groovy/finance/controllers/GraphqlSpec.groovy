package finance.controllers

import finance.Application
import groovy.json.JsonBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import spock.lang.Specification
import java.util.Base64

//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@AutoConfigureMockMvc
@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GraphqlSpec extends BaseControllerSpec {

    @Autowired
    MockMvc mockMvc

    void 'simple GraphQL query test - to verify GraphQL endpoint is working'() {
        when: 'making a simple GraphQL query'
        String token = generateJwtToken(username)
        def simpleQuery = '{ descriptions { descriptionName } }'
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post(createURLWithPort("/graphql"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("Cookie", "token=" + token)
                .content('{"query":"' + simpleQuery + '"}')
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())

        then: 'it should get a response (even if empty)'
        resultActions.andExpect(status().isOk())
        0 * _
    }

    void 'insertTransfer mutation should accept input parameter - TDD failing test'() {
        given: 'transfer data for the test'
        // Use simple account names that match the existing primary/secondary pattern
        def sourceAccount = getPrimaryAccountName()
        def destinationAccount = getSecondaryAccountName()

        // Create the GraphQL mutation that the client expects to work (with 'input' parameter)
        def mutation = '''
            mutation InsertTransfer($input: TransferInput!) {
                insertTransfer(input: $input) {
                    transferId
                    sourceAccount
                    destinationAccount
                    amount
                    activeStatus
                }
            }
        '''

        def variables = [
            input: [
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

        when: 'making the GraphQL request'
        String token = generateJwtToken(username)
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post(createURLWithPort("/graphql"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("Cookie", "token=" + token)
                .content(jsonBuilder.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())

        then: 'it should succeed without parameter name errors'
        resultActions.andExpect(status().isOk())
        resultActions.andExpect(jsonPath('$.data.insertTransfer.sourceAccount').value(sourceAccount))
        resultActions.andExpect(jsonPath('$.data.insertTransfer.destinationAccount').value(destinationAccount))
        resultActions.andExpect(jsonPath('$.data.insertTransfer.amount').value(100.00))
        resultActions.andExpect(jsonPath('$.errors').doesNotExist())
        0 * _
    }

    void 'insertTransfer mutation should accept guidSource and guidDestination fields - TDD failing test'() {
        given: 'transfer data with guid fields that frontend wants to send'
        def sourceAccount = getPrimaryAccountName()
        def destinationAccount = getSecondaryAccountName()
        def testGuidSource = "550e8400-e29b-41d4-a716-446655440001"
        def testGuidDestination = "550e8400-e29b-41d4-a716-446655440002"

        def mutation = '''
            mutation InsertTransfer($input: TransferInput!) {
                insertTransfer(input: $input) {
                    transferId
                    sourceAccount
                    destinationAccount
                    amount
                    guidSource
                    guidDestination
                    activeStatus
                }
            }
        '''

        def variables = [
            input: [
                sourceAccount: sourceAccount,
                destinationAccount: destinationAccount,
                transactionDate: "2024-01-15",
                amount: 100.00,
                guidSource: testGuidSource,
                guidDestination: testGuidDestination,
                activeStatus: true
            ]
        ]

        def requestBody = [
            query: mutation,
            variables: variables
        ]

        def jsonBuilder = new JsonBuilder(requestBody)

        when: 'making the GraphQL request with guidSource and guidDestination'
        String token = generateJwtToken(username)
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post(createURLWithPort("/graphql"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("Cookie", "token=" + token)
                .content(jsonBuilder.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())

        then: 'it should accept guidSource and guidDestination fields without errors'
        resultActions.andExpect(status().isOk())
        resultActions.andExpect(jsonPath('$.data.insertTransfer.sourceAccount').value(sourceAccount))
        resultActions.andExpect(jsonPath('$.data.insertTransfer.destinationAccount').value(destinationAccount))
        resultActions.andExpect(jsonPath('$.data.insertTransfer.amount').value(100.00))
        resultActions.andExpect(jsonPath('$.data.insertTransfer.guidSource').value(testGuidSource))
        resultActions.andExpect(jsonPath('$.data.insertTransfer.guidDestination').value(testGuidDestination))
        resultActions.andExpect(jsonPath('$.errors').doesNotExist())
        0 * _
    }

    void 'insertTransfer mutation should handle empty string guidSource and guidDestination - reproduces UUID validation error'() {
        given: 'transfer data with empty string guid fields that cause validation error'
        def sourceAccount = getPrimaryAccountName()
        def destinationAccount = getSecondaryAccountName()

        def mutation = '''
            mutation InsertTransfer($input: TransferInput!) {
                insertTransfer(input: $input) {
                    transferId
                    sourceAccount
                    destinationAccount
                    amount
                    guidSource
                    guidDestination
                    activeStatus
                }
            }
        '''

        def variables = [
            input: [
                sourceAccount: sourceAccount,
                destinationAccount: destinationAccount,
                transactionDate: "2025-09-27T17:40:44.658Z",
                amount: 2883.45,
                guidSource: "",
                guidDestination: "",
                activeStatus: true
            ]
        ]

        def requestBody = [
            query: mutation,
            variables: variables
        ]

        def jsonBuilder = new JsonBuilder(requestBody)

        when: 'making the GraphQL request with empty string guid fields'
        String token = generateJwtToken(username)
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post(createURLWithPort("/graphql"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("Cookie", "token=" + token)
                .content(jsonBuilder.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())

        then: 'it should succeed without UUID validation errors for empty strings'
        resultActions.andExpect(status().isOk())
        resultActions.andExpect(jsonPath('$.data.insertTransfer.sourceAccount').value(sourceAccount))
        resultActions.andExpect(jsonPath('$.data.insertTransfer.destinationAccount').value(destinationAccount))
        resultActions.andExpect(jsonPath('$.data.insertTransfer.amount').value(2883.45))
        resultActions.andExpect(jsonPath('$.errors').doesNotExist())
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
