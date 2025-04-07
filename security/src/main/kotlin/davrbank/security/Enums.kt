package davrbank.security

enum class Gender {
    MALE,
    FEMALE
}

enum class Role{
    ROLE_USER,ROLE_ADMIN
}


enum class ErrorCode(val code: Int) {
    USER_NOT_FOUND(400),
    USERNAME_ALREADY_EXIST(406),
    GENDER_ENUM_ERROR(402),
    VALIDATION_ERROR(409),
    USER_ROLE_NOT_EXIST(404),
    FORBIDDEN_EXCEPTION(403),
    INVALID_REFRESH_TOKEN(405),
    LOGIN_PASSWORD_ERROR(401)
}