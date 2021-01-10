//package finance.controllers
//
//import finance.Application
//import finance.domain.Parameter
//import finance.services.ParameterApiService
//import org.jetbrains.annotations.NotNull
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
//import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.boot.web.server.LocalServerPort
//import org.springframework.http.MediaType
//import org.springframework.test.context.ActiveProfiles
//import org.springframework.test.web.servlet.MockMvc
//import org.springframework.test.web.servlet.ResultActions
//import org.springframework.test.web.servlet.setup.MockMvcBuilders
//import retrofit2.Call
//import retrofit2.Retrofit
//import retrofit2.http.Path
//import retrofit2.mock.BehaviorDelegate
//import retrofit2.mock.MockRetrofit
//import retrofit2.mock.NetworkBehavior
//import spock.lang.Shared
//import spock.lang.Specification
//import spock.lang.Subject
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
//
//
//@ActiveProfiles("func")
//@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
////@AutoConfigureMockMvc
//class ParameterControllerSpec extends Specification {
//
//    @Autowired
//    ParameterController parameterController
//
//    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(parameterController).build()
//
////    @LocalServerPort
////    private int port
//
//
//    def 'should allow http-basic authentication'() {
//        when:
//        def response = this.mockMvc.perform(post("/v1/product/search")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content('test'))
//                .andReturn().response
////        def result = mockMvc.perform(
////                get("/credentials"))
//
//        then:
//        noExceptionThrown()
//    }
//}
