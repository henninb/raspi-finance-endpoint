package finance.controllers

import finance.services.AccountService
import finance.services.TransactionService
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

//@CrossOrigin(origins = arrayOf("http://localhost:3000"))
//Thymeleaf and RestController will output JSON and not HTML
@Controller
class ThymeleafController {
    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    @Autowired
    lateinit var transactionService: TransactionService

    @Autowired
    lateinit var accountService: AccountService

    //localhost:8080/
    @RequestMapping("/")
    fun index(): String {
        //var model: Model
        return "index"
    }

    //http://localhost:8080/transactionCreate
    @GetMapping(path = [("/transactionCreate")])
    fun transactionCreate(): String {
        return "transactionCreate"
    }

    //ResponseEntity - not sure
    //http://localhost:8080/transactionView
    @GetMapping(path = [("/transactionView")])
    fun transactionView(model: Model): String {
        val accounts = accountService.findAllAcitveAccounts()
        model.addAttribute("accounts", accounts)
        return "transactionView"
    }

//    @RequestMapping(value = "/submitQuery", method = arrayOf(RequestMethod.POST))
//    fun processQuery(@ModelAttribute form: ClientForm, model: Model): String {
//        System.out.println(form.getClientList())
//    }

    //http://localhost:8080/transactionView/amex_brian
    //TODO: ResponseEntity code fix
    @GetMapping(path = [("/transactionView/{accountNameOwner}")])
    fun transactionView(@PathVariable accountNameOwner: String, model: Model): String {
        val accounts = accountService.findAllAcitveAccounts()
        val transactions = transactionService.findByAccountNameOwnerIgnoreCaseOrderByTransactionDate(accountNameOwner)
        model.addAttribute("transactions", transactions)
        model.addAttribute("accounts", accounts)
        return "transactionView"
    }
}
