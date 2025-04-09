package davrbank.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.*
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Component
class JwtUtil(
    jwtProperties: JwtProperties
) {

    private val secretKey = Keys.hmacShaKeyFor(
        jwtProperties.key.toByteArray()
    )

    fun generateToken(user: User, expirationDate: Date): String {
        return Jwts.builder()
            .subject(user.username)
            .claim("role", user.role.name)
            .claim("id",user.id)
            .issuedAt(Date(System.currentTimeMillis()))
            .expiration(expirationDate)
            .signWith(secretKey)
            .compact()
    }

    fun getUsernameFromToken(token: String): String? {
        return getAllClaims(token).subject
    }

    fun isValid(token: String, user: User): Boolean {
        val email = getUsernameFromToken(token)

        return user.username == email && !isExpired(token)
    }

    fun isExpired(token: String): Boolean {
        return getAllClaims(token)
            .expiration
            .before(Date(System.currentTimeMillis()))
    }


    fun getAllClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}

@Configuration
class Configuration {
    @Bean
    fun userDetailsService(userRepository: UserRepository): UserDetailsService =
        CustomUserDetailsService(userRepository)

    @Bean
    fun encoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationProvider(userRepository: UserRepository): AuthenticationProvider =
        DaoAuthenticationProvider()
            .also {
                it.setUserDetailsService(userDetailsService(userRepository))
                it.setPasswordEncoder(encoder())

            }

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager =
        config.authenticationManager

}

@Component
class JwtAuthenticationFilter(
    private val userDetailsService: CustomUserDetailsService,
    private val jwtUtil: JwtUtil,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val authHeader: String? = request.getHeader("Authorization")

            if (authHeader.doesNotContainBearerToken()) {
                filterChain.doFilter(request, response)
                return
            }

            val jwtToken = authHeader!!.extractTokenValue()
            val username = jwtUtil.getUsernameFromToken(jwtToken)

            if (username != null && SecurityContextHolder.getContext().authentication == null) {
                val foundEmployee = userDetailsService.loadUserByUsername(username) as User

                if (jwtUtil.isValid(jwtToken, foundEmployee)) {
                    updateContext(foundEmployee, request)
                }

                filterChain.doFilter(request, response)
            }
        } catch (e: Exception) {
            filterChain.doFilter(request, response)
        }
    }

    private fun updateContext(foundEmployee: UserDetails, request: HttpServletRequest) {
        val authToken = UsernamePasswordAuthenticationToken(foundEmployee, null, foundEmployee.authorities)
        authToken.details = WebAuthenticationDetailsSource().buildDetails(request)

        SecurityContextHolder.getContext().authentication = authToken
    }

    private fun String?.doesNotContainBearerToken(): Boolean = this == null || !this.startsWith("Bearer ")

    private fun String.extractTokenValue(): String = this.substringAfter("Bearer ")
}

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val authenticationProvider: AuthenticationProvider,
    private val customAccessDeniedHandler: CustomAccessDeniedHandler,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) : WebSecurityConfigurerAdapter() {

    override fun configure(http: HttpSecurity) {
        http
            .csrf().disable()
            .authorizeRequests()
            .antMatchers("/register", "/login", "/refresh-token").permitAll() // `antMatchers` Spring Boot 2.3.x uchun
            .anyRequest().authenticated()
            .and()
            .exceptionHandling()
            .accessDeniedHandler(customAccessDeniedHandler)
            .and()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
    }
}



