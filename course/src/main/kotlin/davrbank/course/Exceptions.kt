package davrbank.course

import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.util.stream.Collectors
import kotlin.collections.HashMap


@ControllerAdvice
class ExceptionHandler(
    private val errorMessageSource: ResourceBundleMessageSource
) {

    @ExceptionHandler(CourseServiceException::class)
    fun handleShoppingException(exception: CourseServiceException): ResponseEntity<BaseMessage> {
        return ResponseEntity.badRequest().body(exception.getErrorMessage(errorMessageSource))
    }

    @ExceptionHandler(BindException::class)
    fun validation(e: BindException): ResponseEntity<Any> {
        e.printStackTrace()
        val fields: MutableMap<String, Any?> = HashMap()
        for (fieldError in e.bindingResult.fieldErrors) {
            fields[fieldError.field] = fieldError.defaultMessage
        }

        val errorCode = ErrorCode.VALIDATION_ERROR
        val message = errorMessageSource.getMessage(
            errorCode.toString(),
            null,
            LocaleContextHolder.getLocale()
        )
        return ResponseEntity.badRequest().body(
            ValidationErrorMessage(
                errorCode.code,
                message,
                fields
            )
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun on(e: MethodArgumentNotValidException): ResponseEntity<Any> {
        val fields = e.bindingResult.fieldErrors.stream()
            .map { error ->
                ValidationFieldError(
                    error.field,
                    getErrorMessage(error.defaultMessage ?: ErrorCode.VALIDATION_ERROR.name, null, errorMessageSource)
                )
            }.collect(Collectors.toList())

        val errorCode = ErrorCode.VALIDATION_ERROR
        val message = getErrorMessage(ErrorCode.VALIDATION_ERROR.name, null, errorMessageSource)
        return ResponseEntity.badRequest().body(
            BaseMessage(
                errorCode.code,
                message!!,
                fields
            )
        )
    }

    fun getErrorMessage(
        errorCode: String,
        errorMessageArguments: Array<Any?>?,
        errorMessageSource: ResourceBundleMessageSource
    ): String? {
        val errorMessage = try {
            errorMessageSource.getMessage(errorCode, errorMessageArguments, LocaleContextHolder.getLocale())
        } catch (e: Exception) {
            e.message
        }
        return errorMessage
    }
}

sealed class CourseServiceException(message: String? = null) : RuntimeException(message) {
    abstract fun errorType(): ErrorCode
    protected open fun getErrorMessageArguments(): Array<Any?>? = null
    fun getErrorMessage(errorMessageSource: ResourceBundleMessageSource): BaseMessage {
        return BaseMessage(
            errorType().code,
            errorMessageSource.getMessage(
                errorType().toString(),
                getErrorMessageArguments(),
                LocaleContextHolder.getLocale()
            )
        )
    }
}


data class ValidationErrorMessage(val code: Int, val message: String, val fields: Map<String, Any?>)

    class CourseNameAlreadyExistException : CourseServiceException() {
        override fun errorType() = ErrorCode.COURSE_NAME_ALREADY_EXIST
    }


class CourseNotFoundException : CourseServiceException() {
    override fun errorType() = ErrorCode.COURSE_NOT_FOUND
}

class CourseNotFoundExceptionInList : CourseServiceException() {
    override fun errorType() = ErrorCode.COURSE_NOT_FOUND_IN_LIST
}

