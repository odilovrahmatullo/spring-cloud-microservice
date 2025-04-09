package davrbank.security

import com.fasterxml.jackson.annotation.JsonInclude
import org.hibernate.validator.constraints.Length
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.math.BigDecimal
import javax.validation.constraints.NotBlank


@Component
class JwtProperties {

    @Value("\${jwt.key}")
    lateinit var key: String

    @Value("\${jwt.access-token-expiration}")
    var accessTokenExpiration: Long = 0L

    @Value("\${jwt.refresh-token-expiration}")
    var refreshTokenExpiration: Long = 0L
}


@JsonInclude(JsonInclude.Include.NON_NULL)
data class BaseMessage(
    val code: Int,
    val message: String,
    val fields: MutableList<ValidationFieldError>? = null
)

data class ValidationFieldError(val field: String, val message: String?)


data class UserResponse(
    val id: Long,
    val fullName: String,
    val username: String,
    val gender: Gender,
    val balance: BigDecimal
)

data class TokenResponse(
    val token: String
)

data class RefreshTokenRequest(
    val refreshToken: String
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

data class AuthResponse(
     val accessToken:String,
     val refreshToken:String
)

data class LoginRequest(
    val username:String,
    val password:String
)