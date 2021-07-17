package finance.resolvers

import java.math.BigDecimal
import java.sql.Date

data class Account(
    val accountId: Any?,
    val activeStatus: Boolean,
    val accountNameOwner: String
)

enum class AccountType(val label: String) {
      Credit("Credit"),
      Debit("Debit"),
      Undefined("Undefined");
        
  companion object {
    @JvmStatic
    fun valueOfLabel(label: String): AccountType? {
      return values().find { it.label == label }
    }
  }
}

data class Category(
    val categoryId: Any?,
    val activeStatus: Boolean,
    val category: String
)

data class Description(
    val descriptionId: Any?,
    val activeStatus: Boolean,
    val description: String
)

enum class ImageFormatType(val label: String) {
      Jpeg("Jpeg"),
      Png("Png"),
      Undefined("Undefined");
        
  companion object {
    @JvmStatic
    fun valueOfLabel(label: String): ImageFormatType? {
      return values().find { it.label == label }
    }
  }
}

data class QueryAccountArgs(
    val accountNameOwner: String
) {
  constructor(args: Map<String, Any>) : this(
      args["accountNameOwner"] as String
  )
}
data class Query(
    val descriptions: Iterable<Description>?,
    val categories: Iterable<Category>?,
    val account: Account?,
    val accounts: Iterable<Account>?
)

enum class ReoccurringType(val label: String) {
      Monthly("Monthly"),
      Annually("Annually"),
      BiAnnually("BiAnnually"),
      FortNightly("FortNightly"),
      Quarterly("Quarterly"),
      Onetime("Onetime"),
      Undefined("Undefined");
        
  companion object {
    @JvmStatic
    fun valueOfLabel(label: String): ReoccurringType? {
      return values().find { it.label == label }
    }
  }
}

data class Transaction(
    val transactionId: Any?,
    val guid: String,
    val accountId: Int?,
    val accountType: AccountType,
    val activeStatus: Boolean,
    val transactionDate: Date,
    val accountNameOwner: String,
    val description: String,
    val category: String,
    val Amount: BigDecimal,
    val transactionState: TransactionState,
    val reoccurringType: ReoccurringType,
    val notes: String
)

enum class TransactionState(val label: String) {
      Cleared("Cleared"),
      Outstanding("Outstanding"),
      Future("Future"),
      Undefined("Undefined");
        
  companion object {
    @JvmStatic
    fun valueOfLabel(label: String): TransactionState? {
      return values().find { it.label == label }
    }
  }
}