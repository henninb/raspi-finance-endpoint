package finance.domain

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

fun <T : Any> ServiceResult<T>.toOkResponse(): ResponseEntity<T> =
    when (this) {
        is ServiceResult.Success -> ResponseEntity.ok(data)
        is ServiceResult.NotFound -> ResponseEntity.notFound().build()
        is ServiceResult.ValidationError -> ResponseEntity.badRequest().build()
        is ServiceResult.BusinessError -> ResponseEntity.status(HttpStatus.CONFLICT).build()
        is ServiceResult.SystemError -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
    }

fun <T : Any> ServiceResult<T>.toCreatedResponse(): ResponseEntity<T> =
    when (this) {
        is ServiceResult.Success -> ResponseEntity.status(HttpStatus.CREATED).body(data)
        is ServiceResult.NotFound -> ResponseEntity.notFound().build()
        is ServiceResult.ValidationError -> ResponseEntity.badRequest().build()
        is ServiceResult.BusinessError -> ResponseEntity.status(HttpStatus.CONFLICT).build()
        is ServiceResult.SystemError -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
    }

fun <T : Any> ServiceResult<List<T>>.toListOkResponse(): ResponseEntity<List<T>> =
    when (this) {
        is ServiceResult.Success -> ResponseEntity.ok(data)
        is ServiceResult.NotFound -> ResponseEntity.notFound().build()
        is ServiceResult.ValidationError -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        is ServiceResult.BusinessError -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        is ServiceResult.SystemError -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
    }

fun <T : Any> ServiceResult<Page<T>>.toPagedOkResponse(pageable: Pageable): ResponseEntity<Page<T>> =
    when (this) {
        is ServiceResult.Success -> ResponseEntity.ok(data)
        is ServiceResult.NotFound -> ResponseEntity.notFound().build()
        is ServiceResult.ValidationError -> ResponseEntity.badRequest().build()
        is ServiceResult.BusinessError -> ResponseEntity.badRequest().build()
        is ServiceResult.SystemError -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
    }
