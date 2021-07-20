package finance.resolvers

import java.math.BigDecimal
import java.sql.Date

data class PaymentInput(val paymentId:Long?, val accountNameOwner:String, val amount: BigDecimal?, val transactionDate: Date)
