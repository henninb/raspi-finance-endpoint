package finance.controllers

import finance.domain.Transfer
import finance.services.TransferService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.*

@CrossOrigin
@RestController
@RequestMapping("/transfer", "/api/transfer")
class TransferController(private var transferService: TransferService) : BaseController() {

    @GetMapping("/select", produces = ["application/json"])
    fun selectAllTransfers(): ResponseEntity<List<Transfer>> {
        val Transfers = transferService.findAllTransfers()

        return ResponseEntity.ok(Transfers)
    }

    //curl --header "Content-Type: application/json" -X DELETE http://localhost:8443/transfer/delete/1001
    @DeleteMapping("/delete/{transferId}", produces = ["application/json"])
    fun deleteByTransferId(@PathVariable transferId: Long): ResponseEntity<String> {
        val transferOptional: Optional<Transfer> = transferService.findByTransferId(transferId)

        logger.info("deleteByTransferId controller - $transferId")
        if (transferOptional.isPresent) {
            transferService.deleteByTransferId(transferId)
            return ResponseEntity.ok("transfer deleted")
        }
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "transaction not deleted: $transferId")
    }
}