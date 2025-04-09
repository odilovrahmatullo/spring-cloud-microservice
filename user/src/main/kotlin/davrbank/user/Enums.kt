package davrbank.user

enum class Gender {
    MALE,
    FEMALE
}
enum class Role{
    ROLE_USER,ROLE_ADMIN
}


enum class ErrorCode(val code: Int) {
    USER_NOT_FOUND(100),
    USERNAME_ALREADY_EXIST(101),
    GENDER_ENUM_ERROR(102),
    VALIDATION_ERROR(103),
    GENERAL_API_EXCEPTION(104)
}