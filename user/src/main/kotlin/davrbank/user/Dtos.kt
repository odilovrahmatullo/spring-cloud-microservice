package davrbank.user

import com.fasterxml.jackson.annotation.JsonInclude
import org.hibernate.validator.constraints.Length
import java.math.BigDecimal
import javax.validation.constraints.Min
import javax.validation.constraints.NotBlank
import javax.validation.constraints.PositiveOrZero

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BaseMessage(
    val code: Int,
    val message: String,
    val fields: MutableList<ValidationFieldError>? = null
)

data class ValidationFieldError(val field: String, val message: String?)

data class RequestParams(
    @field: PositiveOrZero(message = "THIS_FIELD_MUST_BE_POSITIVE")
    var page: Int = 0,

    @field:Min(1, message = "SIZE_ERROR_MIN")
    var size: Int = 20,

    var search: String = ""
)

data class UserCreateRequest(
    @field:NotBlank(message = "THIS_FIELD_CANNOT_BE_BLANK")
    @field:Length(max = 50)
    val fullName: String,
    @field:NotBlank(message = "THIS_FIELD_CANNOT_BE_BLANK")
    @field:MaxLength(max = 20)
    val username: String,
    @field:NotBlank(message = "THIS_FIELD_CANNOT_BE_BLANK")
    @field:MaxLength(max = 20)
    val password: String,

    @field:ValidEnum(enumClass = Gender::class, message = "GENDER_ENUM_ERROR")
    val gender: String
) {
    fun toEntity(bcryptPassword: String): User = User(
        fullName, username, bcryptPassword, Gender.valueOf(gender),
        BigDecimal.ZERO,Role.ROLE_USER)
}

data class UserUpdateRequest(
    val fullName: String?,
    val username: String?,
)

data class UserResponse(
    val id: Long,
    val fullName: String,
    val username: String,
    val gender: Gender,
    val balance:BigDecimal
) {
    companion object {
        fun toResponse(user: User) = user.run {
            UserResponse(id!!, fullName, username, gender,balance)
        }
    }
}

data class UpdateBalanceRequest(
    @field:Min(5000, message = "BALANCE_MUST_BE_ABOVE")
    val balance: BigDecimal,
)

data class CourseDto(
    val id:Long?,
    val name: String,
    val description: String,
    val price: BigDecimal,
    val expiredDate:Long,
    val viewCount:Long?
)

data class UserCourseDetails(
    val userId:Long,
    val fullName:String,
    val courses: List<CourseDto>,
    val totalPayment:BigDecimal)
