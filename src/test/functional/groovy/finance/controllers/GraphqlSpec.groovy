package finance.controllers

import finance.Application
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import spock.lang.Specification

//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@AutoConfigureMockMvc
@ActiveProfiles("func")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GraphqlSpec extends BaseControllerSpec {

    @LocalServerPort
    protected int port

    @Autowired
    MockMvc mockMvc

    protected String createURLWithPort(String uri) {
        return "http://localhost:${port}" + uri
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
