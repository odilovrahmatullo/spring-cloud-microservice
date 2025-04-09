package davrbank.course

import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.stereotype.Component
import javax.validation.Constraint
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext
import javax.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [LocalizedStringValidator::class])
annotation class ValidLocalizedString(
    val message: String = "LOCALIZED_STRING_TOO_LONG",
    val max: Int = 255,
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class LocalizedStringValidator : ConstraintValidator<ValidLocalizedString, LocalizedString> {

    private var max: Int = 255

    override fun initialize(constraintAnnotation: ValidLocalizedString) {
        this.max = constraintAnnotation.max
    }

    override fun isValid(value: LocalizedString?, context: ConstraintValidatorContext): Boolean {
        if (value == null) return true

        return listOf(value.en, value.ru, value.uz)
            .all { it.length <= max }
    }
}



@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [MaxLengthValidator::class])
annotation class MaxLength(
    val max: Int,
    val message: String = "THIS_FIELD_LENGTH_ERROR",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

@Component
class MaxLengthValidator(private val messageSource: ResourceBundleMessageSource) : ConstraintValidator<MaxLength, String> {

    private var max: Int = 0

    override fun initialize(constraintAnnotation: MaxLength) {
        this.max = constraintAnnotation.max
    }

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        if (value == null) return true

        val locale = LocaleContextHolder.getLocale() // Foydalanuvchi tilini aniqlash
        val message = messageSource.getMessage("THIS_FIELD_LENGTH_ERROR", arrayOf(max), locale)

        if (value.length > max) {
            context.disableDefaultConstraintViolation()
            context.buildConstraintViolationWithTemplate(message).addConstraintViolation()
            return false
        }

        return true
    }
}


@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [EnumValidator::class])
annotation class ValidEnum(
    val enumClass: KClass<out Enum<*>>,
    val message: String = "Noto‘g‘ri enum qiymati!",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)


class EnumValidator : ConstraintValidator<ValidEnum, String> {
    private lateinit var enumValues: Array<out Enum<*>>

    override fun initialize(annotation: ValidEnum) {
        enumValues = annotation.enumClass.java.enumConstants
    }

    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value == null) return false
        return enumValues.any { it.name == value }
    }
}





