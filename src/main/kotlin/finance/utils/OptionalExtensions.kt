package finance.utils

import jakarta.persistence.EntityNotFoundException
import java.util.Optional

fun <T> Optional<T>.orThrowNotFound(
    entityName: String,
    id: Any? = null,
): T =
    orElseThrow {
        EntityNotFoundException(if (id != null) "$entityName not found: $id" else "$entityName not found")
    }
