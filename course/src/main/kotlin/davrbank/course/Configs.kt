package davrbank.course

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import lombok.extern.slf4j.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.*
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class PageImplDeserializer<T>(private val clazz: Class<T>) : JsonDeserializer<PageImpl<T>>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): PageImpl<T> {
        val mapper = (p.codec as ObjectMapper)
        val node: ObjectNode = mapper.readTree(p)

        val contentNode = node["content"]
        val typeFactory = mapper.typeFactory
        val listType = typeFactory.constructCollectionType(List::class.java, clazz)
        val content: List<T> = mapper.convertValue(contentNode, listType)

        val pageable: Pageable = PageRequest.of(node["number"].asInt(), node["size"].asInt())
        val total = node["totalElements"].asLong()

        return PageImpl(content, pageable, total)
    }
}


@Configuration
class FeignConfiguration {

    @Bean
    fun feignObjectMapper(): ObjectMapper {
        val mapper = ObjectMapper()
        mapper.registerModule(KotlinModule())
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        val module = SimpleModule()
        // TopSellingCourse uchun deserializer qo‘shdim
        module.addDeserializer(
            PageImpl::class.java,
            PageImplDeserializer(TopCourse::class.java) // agar yana boshqa page olmoqchi busam shu yerga qushishim kerak
        )
        mapper.registerModule(module)

        return mapper
    }
}


@EnableWebSecurity
@Slf4j
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Configuration
class SecurityConfig(
    private val jwtTokenUtil: JwtTokenUtil,
    private val accessDeniedHandler: CustomAccessDeniedHandler
) : WebSecurityConfigurerAdapter() {

    override fun configure(http: HttpSecurity) {
        http.csrf().disable()
        http
            .authorizeRequests()
            .antMatchers("/internal/**", "/list", "/most-sold","/most-viewed").permitAll()
            .anyRequest().authenticated()
            .and()
            .exceptionHandling().accessDeniedHandler(accessDeniedHandler)

        http.addFilterBefore(
            JwtAuthenticationFilter(jwtTokenUtil, accessDeniedHandler),
            UsernamePasswordAuthenticationFilter::class.java
        )
    }
}


@Slf4j
class JwtAuthenticationFilter(
    private val jwtTokenUtil: JwtTokenUtil,
    private val accessDeniedHandler: CustomAccessDeniedHandler
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val openEndpoints = listOf("/internal/**", "/list", "/most-sold","/most-viewed")
        val matcher = org.springframework.util.AntPathMatcher()

        return openEndpoints.any { matcher.match(it, request.requestURI) }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {

        val token = request.getHeader("Authorization")?.let {
            if (it.startsWith("Bearer ")) it.substring(7) else null
        }

        if (token == null || !jwtTokenUtil.validateToken(token)) {
            accessDeniedHandler.setForbiddenResponse(response)
            return
        }

        try {
            val claims = jwtTokenUtil.decodeJwt(token)
            val username = claims.subject
            val roles = claims["role"]
            logger.info("Decoded roles: $roles")

            val authorities = if (roles is String) {
                listOf(SimpleGrantedAuthority(roles))
            } else if (roles is List<*>) {
                roles.map { SimpleGrantedAuthority(it.toString()) }
            } else {
                emptyList<SimpleGrantedAuthority>()
            }

            val authentication = UsernamePasswordAuthenticationToken(username, token, authorities)
            authentication.details = WebAuthenticationDetailsSource().buildDetails(request)

            SecurityContextHolder.getContext().authentication = authentication
            logger.info("Foydalanuvchi autentifikatsiya qilindi: $username")
        } catch (e: Exception) {
            logger.warn("Token validatsiyasida xatolik: ${e.message}")
            accessDeniedHandler.setForbiddenResponse(response)
            return
        }

        filterChain.doFilter(request, response)
    }
}


@Component
class JwtTokenUtil {

    @Value("\${jwt.key}")
    lateinit var key: String

    companion object {
        private val log: Logger = LoggerFactory.getLogger(JwtTokenUtil::class.java)
    }

    fun decodeJwt(token: String): Claims {
        try {
            val secretKey = Keys.hmacShaKeyFor(key.toByteArray())
            val claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
            return claims.payload
        } catch (e: Exception) {
            log.info("Tokenni dekodlashda xatolik: ${e.message}")
            throw IllegalArgumentException("Invalid token", e)
        }
    }

    fun validateToken(token: String): Boolean {
        val claims = decodeJwt(token)
        return !isTokenExpired(claims)
    }

    fun isTokenExpired(claims: Claims): Boolean {
        return claims.expiration.before(Date())
    }
}