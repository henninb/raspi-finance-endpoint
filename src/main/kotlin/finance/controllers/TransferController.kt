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
        val transfers = transferService.findAllTransfers()

        return ResponseEntity.ok(transfers)
    }

    @PostMapping("/insert", produces = ["application/json"])
    fun insertTransfer(@RequestBody transfer: Transfer): ResponseEntity<Transfer> {
        return try {
            val transferResponse = transferService.insertTransfer(transfer)
            ResponseEntity.ok(transferResponse)
        } catch (ex: ResponseStatusException) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to insert transfer: ${ex.message}", ex)
        } catch (ex: Exception) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: ${ex.message}", ex)
        }
    }

    //curl --header "Content-Type: application/json" -X DELETE http://localhost:8443/transfer/delete/1001
    @DeleteMapping("/delete/{transferId}", produces = ["application/json"])
    fun deleteByTransferId(@PathVariable transferId: Long): ResponseEntity<Transfer> {
        val transferOptional: Optional<Transfer> = transferService.findByTransferId(transferId)

        if (transferOptional.isPresent) {
            val transfer: Transfer = transferOptional.get()
            logger.info("transfer deleted: ${transfer.transferId}")
            transferService.deleteByTransferId(transferId)
            return ResponseEntity.ok(transfer)
        }
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "transaction not deleted: $transferId")
    }
}