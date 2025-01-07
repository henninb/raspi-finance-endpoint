package finance.services

import finance.domain.Transfer
//import finance.domain.Transaction
import java.util.*

interface ITransferService {
    fun findAllTransfers(): List<Transfer>

    fun insertTransfer(transfer: Transfer): Transfer
//
//    fun populateDebitTransaction(
//        transactionDebit: Transaction,
//        transfer: Transfer,
//        transferAccountNameOwner: String
//    )
//
//    fun populateCreditTransaction(
//        transactionCredit: Transaction,
//        transfer: Transfer,
//        transferAccountNameOwner: String
//    )

    fun deleteByTransferId(transferId: Long): Boolean
    fun findByTransferId(transferId: Long): Optional<Transfer>
}