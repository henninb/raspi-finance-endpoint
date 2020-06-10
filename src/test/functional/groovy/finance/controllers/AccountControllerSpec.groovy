package finance.controllers

import finance.Application
import finance.domain.Transaction
import finance.services.AccountService
import finance.services.CategoryService
import finance.services.TransactionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared
import spock.lang.Specification

@ActiveProfiles("stage")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountControllerSpec extends Specification {

    @LocalServerPort
    protected int port

    TestRestTemplate restTemplate = new TestRestTemplate()

    @Shared
    HttpHeaders headers

    @Autowired
    TransactionService transactionService

    @Autowired
    AccountService accountService

    @Autowired
    CategoryService categoryService

    @Shared
    Transaction account

}
