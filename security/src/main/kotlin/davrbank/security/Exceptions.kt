package davrbank.security

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.http.ResponseEntity
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.util.stream.Collectors
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@ControllerAdvice
class ExceptionHandler(
    private val errorMessageSource: ResourceBundleMessageSource
) {

    @ExceptionHandler(UserServiceException::class)
    fun handleShoppingException(exception: UserServiceException): ResponseEntity<BaseMessage> {
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

sealed class UserServiceException(message: String? = null) : RuntimeException(message) {
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

@Component
class CustomAccessDeniedHandler(
    private val errorMessageSource: ResourceBundleMessageSource
) : AccessDeniedHandler {
    override fun handle(
        request: HttpServletRequest?,
        response: HttpServletResponse?,
        accessDeniedException: org.springframework.security.access.AccessDeniedException?
    ) {
        val errorCode = ErrorCode.FORBIDDEN_EXCEPTION
        val message = errorMessageSource.getMessage(
            errorCode.toString(),
            null,
            LocaleContextHolder.getLocale()
        )

        val responseBody = BaseMessage(errorCode.code, message)

        response?.status = HttpServletResponse.SC_FORBIDDEN
        response?.contentType = "application/json"
        response?.characterEncoding = "UTF-8"
        response?.writer?.write(ObjectMapper().writeValueAsString(responseBody))
    }
}


data class ValidationErrorMessage(val code: Int, val message: String, val fields: Map<String, Any?>)

class UsernameAlreadyExistException : UserServiceException() {
    override fun errorType() = ErrorCode.USERNAME_ALREADY_EXIST
}

class UserNotFoundException : UserServiceException() {
    override fun errorType() = ErrorCode.USER_NOT_FOUND
}

class UserRoleNotExistException : UserServiceException(){
    override fun errorType() = ErrorCode.USER_ROLE_NOT_EXIST
}

class ForbiddenException : UserServiceException(){
    override fun errorType() = ErrorCode.FORBIDDEN_EXCEPTION
}

class RefreshTokenException : UserServiceException(){
    override fun errorType() = ErrorCode.INVALID_REFRESH_TOKEN
}
class AuthServiceException : UserServiceException(){
    override fun errorType() = ErrorCode.LOGIN_PASSWORD_ERROR
}