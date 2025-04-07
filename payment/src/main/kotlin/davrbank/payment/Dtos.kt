package davrbank.payment

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Date
import javax.validation.constraints.Min
import javax.validation.constraints.Positive
import javax.validation.constraints.PositiveOrZero

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BaseMessage(
    val code: Int?=null,
    val message: String?=null,
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

data class PaymentCreateRequest(
    @field:Positive(message = "THIS_FIELD_MUST_BE_POSITIVE")
    val courseId:Long,
    @field:Positive(message = "THIS_FIELD_MUST_BE_POSITIVE")
    val money: BigDecimal,
) {
    fun toEntity(userId:Long): Payment = Payment(courseId,userId,money,PaymentStatus.PAID)
}


data class UserResponse(
    val id: Long,
    val fullName: String,
    val username: String,
    val gender: Gender,
    val balance:BigDecimal
)

data class CourseResponse(
    val id:Long?,
    val name: String,
    val description: String,
    val price: BigDecimal,
    val expiredDate:Long,
    val viewCount:Long?
)

data class PaymentResponse(
    val id:Long,
    val userResponse:UserResponse,
    val courseResponse: CourseResponse,
    val paidMoney:BigDecimal,
    val paymentStatus:PaymentStatus,
    val createdDate:Date

){
    companion object {
        fun toResponse(user:UserResponse,course: CourseResponse,payment:Payment)
         = PaymentResponse(payment.id!!,user, course,payment.paidMoney,payment.paymentStatus,payment.createdDate!!)
    }
}

data class TopSellingCourse(
    val courseId:Long,
    val count:Long
)
data class PaymentFilterDto(
    @field:JsonFormat(pattern = "dd-MM-yyyy")
    val fromDate:LocalDate?,
    @field:JsonFormat(pattern = "dd-MM-yyyy")
    val toDate:LocalDate?
)




