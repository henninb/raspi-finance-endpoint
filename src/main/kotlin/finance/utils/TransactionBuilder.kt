package finance.utils

import finance.models.Transaction
import finance.pojos.AccountType
import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp

class TransactionBuilder {
    fun buildTransaction(): Transaction {
        //val transaction = Transaction.Builder()
        var transaction = Transaction()

        transaction.transactionId = 1002
        transaction. guid = "4ea3be58-3993-46de-88a2-4ffc7f1d73bd"
        transaction. accountId = 1004
        transaction. accountType = AccountType.valueOf("Credit")
        transaction. accountNameOwner = "brian_chase"
        transaction. transactionDate =  Date(1553645394)
        transaction. description = "aliexpress.com"
        transaction. category = "online"
        transaction. amount = BigDecimal(3.14)
        transaction. cleared = 1
        transaction. reoccurring = false
        transaction. notes = "my note to you"
        transaction. dateUpdated =  Timestamp(1553645394000)
        transaction. dateAdded =  Timestamp(1553645394000)
        transaction. sha256 = "963e35c37ea59f3f6fa35d72fb0ba47e1e1523fae867eeeb7ead64b55ff22b77"
        return transaction
    }
}