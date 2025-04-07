package davrbank.security

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.util.*
import javax.transaction.Transactional

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        return userRepository.findByUsernameAndDeletedFalse(username) ?: throw UserNotFoundException()
    }
}

@FeignClient("user", path = "/internal")
interface UserService {
    @GetMapping("exists/{id}")
    fun existById(@PathVariable id: Long): Boolean

    @GetMapping("{id}")
    fun getUserById(@PathVariable id: Long): UserResponse

    @GetMapping("{username}")
    fun getUserByUsername(@PathVariable username: String): UserResponse

    @PostMapping()
    fun create(@RequestBody request: UserCreateRequest)

}

interface AuthService {
    fun signIn(request: UserCreateRequest)
    fun login(loginRequest: LoginRequest):AuthResponse
    fun refreshToken(request: RefreshTokenRequest):String?
}


@Service
class AuthServiceImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProperties: JwtProperties,

) : AuthService {

    @Transactional
    override fun signIn(request: UserCreateRequest) {
        request.run {
            if(userRepository.existsByUsername(username)) throw UsernameAlreadyExistException()
           userRepository.save(toEntity(bcryptPassword = passwordEncoder.encode(request.password)))
        }
    }

    override fun login(loginRequest: LoginRequest):AuthResponse {
        val user = userRepository.findByUsernameAndDeletedFalse(loginRequest.username) ?:throw AuthServiceException()

        if (!passwordEncoder.matches(loginRequest.password, user.password)) {
            throw AuthServiceException()
        }

        val accessToken = generateAccessToken(user)
        val refreshToken = generateRefreshToken(user)

        refreshTokenRepository.save(RefreshToken(user=user, refreshToken = refreshToken))

        return AuthResponse(accessToken, refreshToken)
    }

    private fun generateRefreshToken(user: User) = jwtUtil.generateToken(
        user = user,
        expirationDate = Date(System.currentTimeMillis() + jwtProperties.refreshTokenExpiration)
    )

    private fun generateAccessToken(user: User) = jwtUtil.generateToken(
        user = user,
        expirationDate = Date(System.currentTimeMillis() + jwtProperties.accessTokenExpiration)
    )


    override fun refreshToken(request: RefreshTokenRequest): String? {
        val extractedUsername = jwtUtil.getUsernameFromToken(request.refreshToken) ?: throw UserNotFoundException()

        val currentUser = userRepository.findByUsernameAndDeletedFalse(extractedUsername)
            ?: throw UserNotFoundException()

        val refreshTokenUser = refreshTokenRepository.findByRefreshTokenAndDeletedFalse(request.refreshToken)
            ?: return null

        return if (!jwtUtil.isExpired(token = request.refreshToken) && currentUser.username == refreshTokenUser.user.username) {
            generateAccessToken(currentUser)
        } else {
            null
        }
    }

}





