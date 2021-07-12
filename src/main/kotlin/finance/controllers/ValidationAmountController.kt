package finance.controllers

import finance.domain.ValidationAmount
import finance.services.ValidationAmountService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("/validation/amount")
class ValidationAmountController(private var validationAmountService: ValidationAmountService) : BaseController() {

    @PostMapping("/insert/{accountNameOwner}", produces = ["application/json"])
    fun insertValidationAmount(@RequestBody validationAmount: ValidationAmount, @PathVariable("accountNameOwner") accountNameOwner : String) : ResponseEntity<String> {
        //val validationAmount = validationAmountService.findValidationAmountByAccountNameOwner(accountNameOwner)

        validationAmountService.insertValidationAmount(accountNameOwner, validationAmount)
        logger.info("validationAmount inserted")
        return ResponseEntity.ok("validationAmount inserted")
    }

    //curl -k https://hornsup:8080/validation/amount/select/amazon_brian
    @GetMapping("/select/{accountNameOwner}")
    fun selectValidationAmountByAccountId(@PathVariable("accountNameOwner") accountNameOwner: String): ResponseEntity<String> {
        val validationAmount = validationAmountService.findValidationAmountByAccountNameOwner(accountNameOwner)
        logger.info(mapper.writeValueAsString(validationAmount))
        return ResponseEntity.ok(mapper.writeValueAsString(validationAmount))
    }
}
