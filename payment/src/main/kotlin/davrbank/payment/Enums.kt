package davrbank.payment



enum class ErrorCode(val code: Int) {
    VALIDATION_ERROR(500),
    PAYMENT_NOT_FOUND(501),
    COURSE_NOT_FOUND(502),
    USER_NOT_FOUND(504),
    GENERAL_API_EXCEPTION(503),
    BIG_MONEY(505),
    NOT_ENOUGH_MONEY(506),
    PAYMENT_EXIST(507),
    FORBIDDEN_ERROR(508),
    LESS_MONEY(509),
    INVALID_DATE_FORMAT(510),
    INVALID_DATE(511)
}

enum class Gender{
    MALE,FEMALE
}

enum class PaymentStatus{
    IN_PROCESS,PAID
}