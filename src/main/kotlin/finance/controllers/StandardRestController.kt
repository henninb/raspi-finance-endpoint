package finance.controllers

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.server.ResponseStatusException
import java.util.Optional

/**
 * Standardized REST controller patterns and conventions.
 * All controllers should follow these patterns for consistency.
 *
 * STANDARDIZATION RULES:
 * 1. Exception Handling: Comprehensive, specific exception types with consistent error responses
 * 2. Method Naming: Consistent patterns (findAll, findById, save, update, deleteById)
 * 3. Parameter Naming: camelCase only, no snake_case annotations
 * 4. Empty Results: Return empty lists, never throw 404 for list operations
 * 5. HTTP Status Codes: 200 OK, 201 CREATED, 204 NO_CONTENT, 404 NOT_FOUND, 400 BAD_REQUEST, 409 CONFLICT, 500 INTERNAL_SERVER_ERROR
 * 6. Request Bodies: Use entity types directly, no Map<String, Any>
 * 7. Response Bodies: Return entity objects, use specialized response objects only when necessary
 * 8. Path Variables: Use camelCase names without @PathVariable annotations when names match
 * 9. Logging: Consistent debug/info/warn/error patterns
 * 10. Endpoint Patterns: RESTful /api/{entity}/[collection operations] and /api/{entity}/{id}/[single entity operations]
 */
interface StandardRestController<T : Any, ID : Any> {
    /**
     * Standard collection retrieval - never throws 404, always returns list (may be empty)
     * GET /api/{entity}/active
     */
    fun findAllActive(): ResponseEntity<List<T>>

    /**
     * Standard single entity retrieval - throws 404 if not found
     * GET /api/{entity}/{id}
     */
    fun findById(id: ID): ResponseEntity<T>

    /**
     * Standard entity creation - returns 201 CREATED
     * POST /api/{entity}
     */
    fun save(entity: T): ResponseEntity<T>

    /**
     * Standard entity update - returns 200 OK
     * PUT /api/{entity}/{id}
     */
    fun update(
        id: ID,
        entity: T,
    ): ResponseEntity<T>

    /**
     * Standard entity deletion - returns 200 OK with deleted entity
     * DELETE /api/{entity}/{id}
     */
    fun deleteById(id: ID): ResponseEntity<T>

    /**
     * Standard paginated collection retrieval - optional implementation
     * GET /api/{entity}/active/paged?page=0&size=50
     *
     * Default implementation throws UnsupportedOperationException.
     * Override this method to provide pagination support for the entity.
     */
    fun findAllActivePaged(pageable: Pageable): ResponseEntity<Page<T>> = throw UnsupportedOperationException("Pagination not implemented for this entity")
}

/**
 * Standard exception handling patterns that all controllers should use.
 * Provides consistent error responses and logging.
 */
abstract class StandardizedBaseController : BaseController() {
    /**
     * Standard CRUD exception handler with comprehensive coverage.
     * All controllers should use this pattern for consistent error handling.
     */
    protected fun <T : Any> handleCrudOperation(
        operationName: String,
        entityId: Any?,
        operation: () -> T,
    ): ResponseEntity<T> =
        try {
            val result = operation()
            logSuccessfulOperation(operationName, entityId)
            ResponseEntity.ok(result)
        } catch (ex: org.springframework.dao.DataIntegrityViolationException) {
            logOperationError(operationName, entityId, "Data integrity violation", ex)
            throw ResponseStatusException(HttpStatus.CONFLICT, "Operation failed due to data conflict: ${ex.message}")
        } catch (ex: jakarta.validation.ValidationException) {
            logOperationError(operationName, entityId, "Validation error", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Validation error: ${ex.message}")
        } catch (ex: IllegalArgumentException) {
            logOperationError(operationName, entityId, "Invalid input", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input: ${ex.message}")
        } catch (ex: jakarta.persistence.EntityNotFoundException) {
            logOperationError(operationName, entityId, "Entity not found", ex)
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Entity not found: ${ex.message}")
        } catch (ex: java.util.concurrent.ExecutionException) {
            // Handle wrapped exceptions from resilience4j
            val cause = ex.cause
            when (cause) {
                is jakarta.persistence.EntityNotFoundException -> {
                    logOperationError(operationName, entityId, "Entity not found (wrapped)", cause)
                    throw ResponseStatusException(HttpStatus.NOT_FOUND, "Entity not found: ${cause.message}")
                }

                else -> {
                    logOperationError(operationName, entityId, "Execution error", ex)
                    throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Operation failed: ${ex.message}")
                }
            }
        } catch (ex: ResponseStatusException) {
            // Re-throw ResponseStatusException as-is
            throw ex
        } catch (ex: Exception) {
            logOperationError(operationName, entityId, "Unexpected error", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: ${ex.message}")
        }

    /**
     * Standard creation operation handler - returns 201 CREATED
     */
    protected fun <T : Any> handleCreateOperation(
        entityType: String,
        entityId: Any?,
        operation: () -> T,
    ): ResponseEntity<T> =
        try {
            val result = operation()
            logger.info("$entityType created successfully: $entityId")
            ResponseEntity.status(HttpStatus.CREATED).body(result)
        } catch (ex: org.springframework.dao.DataIntegrityViolationException) {
            logger.error("Failed to create $entityType due to data integrity violation: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.CONFLICT, "Duplicate $entityType found")
        } catch (ex: jakarta.validation.ValidationException) {
            logger.error("Validation error creating $entityType $entityId: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Validation error: ${ex.message}")
        } catch (ex: IllegalArgumentException) {
            logger.error("Invalid input creating $entityType $entityId: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input: ${ex.message}")
        } catch (ex: ResponseStatusException) {
            // Re-throw ResponseStatusException as-is
            throw ex
        } catch (ex: Exception) {
            logger.error("Unexpected error creating $entityType $entityId: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: ${ex.message}")
        }

    /**
     * Standard deletion operation handler - returns 200 OK with deleted entity
     */
    protected fun <T : Any> handleDeleteOperation(
        entityType: String,
        entityId: Any?,
        findOperation: () -> Optional<T>,
        deleteOperation: () -> Unit,
    ): ResponseEntity<T> =
        try {
            logger.info("Attempting to delete $entityType: $entityId")
            val entity =
                findOperation().orElseThrow {
                    logger.warn("$entityType not found for deletion: $entityId")
                    ResponseStatusException(HttpStatus.NOT_FOUND, "$entityType not found: $entityId")
                }

            deleteOperation()
            logger.info("$entityType deleted successfully: $entityId")
            ResponseEntity.ok(entity)
        } catch (ex: ResponseStatusException) {
            throw ex
        } catch (ex: Exception) {
            logger.error("Failed to delete $entityType $entityId: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete $entityType: ${ex.message}")
        }

    private fun logSuccessfulOperation(
        operation: String,
        entityId: Any?,
    ) {
        logger.info("$operation completed successfully: $entityId")
    }

    private fun logOperationError(
        operation: String,
        entityId: Any?,
        errorType: String,
        ex: Exception,
    ) {
        logger.error("$operation failed for $entityId - $errorType: ${ex.message}", ex)
    }
}
