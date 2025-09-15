package finance.domain

data class MergeDescriptionsRequest(
    val sourceNames: List<String> = listOf(),
    val targetName: String = ""
)