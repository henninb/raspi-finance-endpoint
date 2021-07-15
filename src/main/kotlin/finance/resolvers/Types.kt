package finance.resolvers

data class Account(
    val accountId: Any?,
    val activeStatus: Boolean?,
    val accountNameOwner: String?
)

enum class AccountType(val label: String) {
      Credit("CREDIT"),
      Debit("DEBIT");
        
  companion object {
    @JvmStatic
    fun valueOfLabel(label: String): AccountType? {
      return values().find { it.label == label }
    }
  }
}

data class Category(
    val categoryId: Any?,
    val activeStatus: Boolean?,
    val category: String?
)

data class Description(
    val descriptionId: Any?,
    val activeStatus: Boolean?,
    val description: String
)

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