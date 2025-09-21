package finance.domain

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper

data class MergeDescriptionsRequest(
    @JsonProperty("sourceNames") val sourceNames: List<String> = listOf(),
    @JsonProperty("targetName") val targetName: String = ""
) {
    override fun toString(): String {
        return mapper.writeValueAsString(this)
    }

    companion object {
        private val mapper = ObjectMapper()
    }
}