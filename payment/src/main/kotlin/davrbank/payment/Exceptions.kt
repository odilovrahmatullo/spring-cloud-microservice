package davrbank.payment

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
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

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(ex: HttpMessageNotReadableException): ResponseEntity<BaseMessage> {
        return if (ex.cause is InvalidFormatException && ex.message?.contains("LocalDate") == true) {
            val error = InvalidDateFormatException()
            val message = error.getErrorMessage(errorMessageSource)
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message)
        } else {
            val message = BaseMessage(400, "So‘rov noto‘g‘ri tuzilgan")
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message)
        }
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
                val messageArgs = error.arguments?.drop(1)?.toTypedArray() // Dinamik argumentlarni olish
                ValidationFieldError(
                    error.field,
                    getErrorMessage(
                        error.defaultMessage ?: ErrorCode.VALIDATION_ERROR.name,
                        messageArgs,
                        errorMessageSource
                    )
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


sealed class PaymentServiceException(message: String? = null) : RuntimeException(message) {
    abstract fun errorType(): ErrorCode

    fun getErrorMessage(errorMessageSource: ResourceBundleMessageSource, vararg array: Any?): BaseMessage {
        return BaseMessage(
            errorType().code,
            errorMessageSource.getMessage(
                errorType().toString(),
                array,
                LocaleContextHolder.getLocale()
            )
        )
    }
}


data class ValidationErrorMessage(val code: Int, val message: String, val fields: Map<String, Any?>)


class PaymentNotFoundException : PaymentServiceException() {
    override fun errorType() = ErrorCode.PAYMENT_NOT_FOUND
}

class GeneralApiException(val msg: String) : PaymentServiceException() {
    override fun errorType(): ErrorCode = ErrorCode.GENERAL_API_EXCEPTION
}

class FeignErrorException(val code: Int?, val errorMessage: String?) : PaymentServiceException() {
    override fun errorType() = ErrorCode.GENERAL_API_EXCEPTION
}

class BigMoneyException : PaymentServiceException() {
    override fun errorType() = ErrorCode.BIG_MONEY
}

class LessMoneyException : PaymentServiceException() {
    override fun errorType() = ErrorCode.LESS_MONEY
}

class NotEnoughMoneyException : PaymentServiceException() {
    override fun errorType() = ErrorCode.NOT_ENOUGH_MONEY
}

class PaymentExistException() : PaymentServiceException() {
    override fun errorType() = ErrorCode.PAYMENT_EXIST
}


class ForbiddenException() : PaymentServiceException() {
    override fun errorType() = ErrorCode.FORBIDDEN_ERROR
}

class InvalidDateFormatException : PaymentServiceException("Invalid date format") {
    override fun errorType(): ErrorCode = ErrorCode.INVALID_DATE_FORMAT
}
class InvalidDateException: PaymentServiceException() {
    override fun errorType(): ErrorCode = ErrorCode.INVALID_DATE
}
