package finance.controllers

data class MergeDescriptionsRequest(
    val sourceNames: List<String> = listOf(),
    val targetName: String = ""
)
