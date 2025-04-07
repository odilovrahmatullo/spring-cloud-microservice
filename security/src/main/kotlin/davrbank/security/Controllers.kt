package davrbank.security

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid


@RestController
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
    fun register(@RequestBody @Valid request: UserCreateRequest) {
        ResponseEntity.ok(authService.signIn(request))
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        return ResponseEntity.ok(authService.login(request))
    }

    @PostMapping("/refresh-token")
    fun refreshToken(@RequestBody request: RefreshTokenRequest): TokenResponse =
        authService.refreshToken(request)?.mapToTokenResponse() ?: throw ForbiddenException()

    private fun String.mapToTokenResponse(): TokenResponse = TokenResponse(
        token = this
    )

}