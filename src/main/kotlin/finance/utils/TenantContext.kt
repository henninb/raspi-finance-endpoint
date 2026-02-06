package finance.utils

import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder

object TenantContext {
    fun getCurrentOwner(): String {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw AccessDeniedException("Not authenticated")
        val username = auth.principal as? String
            ?: throw AccessDeniedException("Invalid principal")
        return username.lowercase()
    }
}
