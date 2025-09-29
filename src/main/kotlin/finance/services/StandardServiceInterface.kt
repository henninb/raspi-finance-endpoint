package finance.services

import finance.domain.ServiceResult

/**
 * Standard CRUD operations interface following controller patterns
 * Provides a consistent contract for all domain services with standardized error handling
 *
 * @param T The entity type
 * @param ID The entity identifier type
 */
interface StandardServiceInterface<T, ID> {
    /**
     * Finds all active entities
     * @return ServiceResult containing list of active entities, or appropriate error
     */
    fun findAllActive(): ServiceResult<List<T>>

    /**
     * Finds entity by its identifier
     * @param id The entity identifier
     * @return ServiceResult containing the entity if found, NotFound error otherwise
     */
    fun findById(id: ID): ServiceResult<T>

    /**
     * Saves a new entity
     * @param entity The entity to save
     * @return ServiceResult containing the saved entity with generated ID, or appropriate error
     */
    fun save(entity: T): ServiceResult<T>

    /**
     * Updates an existing entity
     * @param entity The entity to update
     * @return ServiceResult containing the updated entity, NotFound if entity doesn't exist, or appropriate error
     */
    fun update(entity: T): ServiceResult<T>

    /**
     * Deletes entity by its identifier
     * @param id The entity identifier
     * @return ServiceResult containing true if deleted successfully, NotFound if entity doesn't exist, or appropriate error
     */
    fun deleteById(id: ID): ServiceResult<Boolean>
}
