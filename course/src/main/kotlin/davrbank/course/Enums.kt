package davrbank.course


enum class ErrorCode(val code: Int) {
    COURSE_NOT_FOUND(200),
    COURSE_NAME_ALREADY_EXIST(201),
    VALIDATION_ERROR(202),
    COURSE_NOT_FOUND_IN_LIST(203),
}