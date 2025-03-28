package finance.controllers

import finance.domain.PendingTransaction
import finance.services.PendingTransactionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@CrossOrigin
@RestController
@RequestMapping("/pending/transaction", "/api/pending/transaction")
class PendingTransactionController(private val pendingTransactionService: PendingTransactionService) : BaseController() {

    @PostMapping("/insert", consumes = ["application/json"], produces = ["application/json"])
    fun insertPendingTransaction(@RequestBody pendingTransaction: PendingTransaction): ResponseEntity<PendingTransaction> {
        return try {
            val response = pendingTransactionService.insertPendingTransaction(pendingTransaction)
            ResponseEntity.ok(response)
        } catch (ex: Exception) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to insert pending transaction: ${ex.message}", ex)
        }
    }

    @DeleteMapping("/delete/{id}")
    fun deletePendingTransaction(@PathVariable id: Long): ResponseEntity<Void> {
        return if (pendingTransactionService.deletePendingTransaction(id)) {
            ResponseEntity.noContent().build()
        } else {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to delete pending transaction with ID: $id")
        }
    }

    @GetMapping("/all", produces = ["application/json"])
    fun getAllPendingTransactions(): ResponseEntity<List<PendingTransaction>> {
        val transactions = pendingTransactionService.getAllPendingTransactions()
        if (transactions.isEmpty()) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "No pending transactions found.")
        }
        return ResponseEntity.ok(transactions)
    }

    @DeleteMapping("/delete/all")
    fun deleteAllPendingTransactions(): ResponseEntity<Void> {
        return try {
            pendingTransactionService.deleteAllPendingTransactions()
            ResponseEntity.noContent().build()
        } catch (ex: Exception) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to delete all pending transactions: ${ex.message}",
                ex
            )
        }
    }
}