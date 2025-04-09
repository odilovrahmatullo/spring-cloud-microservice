package davrbank.course

import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal
import javax.validation.constraints.Min
import javax.validation.constraints.NotBlank
import javax.validation.constraints.PositiveOrZero

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BaseMessage(
    val code: Int?,
    val message: String?,
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

data class CourseDto(
    val id:Long?,
    @field:NotBlank(message = "THIS_FIELD_CANNOT_BE_BLANK")
    @field:MaxLength(max = 50)
    val name: String,
    @field:MaxLength(max = 512)
    val description: String,
    @field: PositiveOrZero(message = "THIS_FIELD_MUST_BE_POSITIVE")
    val price: BigDecimal,
    @field: PositiveOrZero(message = "THIS_FIELD_MUST_BE_POSITIVE")
    val expiredDate:Long,
    val viewCount:Long?
) {
    fun toEntity(): Course = Course(name, description, price, expiredDate)

    companion object{
        fun toResponse(course:Course,viewCount:Long):CourseDto = course.run {
            CourseDto(id,name,description,price,expiredDate,viewCount)
        }
    }
}

data class CourseUpdateRequest(
    @field:MaxLength(max = 50)
    val name: String?,
    @field:MaxLength(max = 512)
    val description: String?,
    @field: PositiveOrZero(message = "THIS_FIELD_MUST_BE_POSITIVE")
    val price: BigDecimal?
)

data class TopCourse(
    val courseId:Long,
    val count:Long
)

data class TopSellingDetailResponse(
    val courses: CourseDto,
    val soldCount:Long
)
