package davrbank.course

import lombok.extern.slf4j.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Configuration
class WebMvcConfig : WebMvcConfigurer {

    @Bean
    fun localeResolver(): LocaleResolver {
        val localeResolver = AcceptHeaderLocaleResolver()
        localeResolver.defaultLocale = Locale("uz")
        return localeResolver
    }

    @Bean
    fun errorMessageSource() = ResourceBundleMessageSource().apply {
        setDefaultEncoding(Charsets.UTF_8.name())
        setUseCodeAsDefaultMessage(true)
        setBasename("error")
    }

}


@Component
class CustomAccessDeniedHandler(
    private val errorMessageSource: ResourceBundleMessageSource
) : AccessDeniedHandler {

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException
    ) {
        setForbiddenResponse(response)
    }

    fun setForbiddenResponse(response: HttpServletResponse) {
        val locale = LocaleContextHolder.getLocale()
        val errorMessage = errorMessageSource.getMessage("FORBIDDEN_ERROR", null, locale)

        response.status = HttpStatus.FORBIDDEN.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write(
            """
            {
                "code": ${HttpStatus.FORBIDDEN.value()},
                "message": "$errorMessage"
            }
            """.trimIndent()
        )
    }


}

@Component
@Slf4j
class SecurityUtil(private val jwtTokenUtil: JwtTokenUtil) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SecurityUtil::class.java)
    }

    fun getCurrentUserId(): Long? {
        val authentication = SecurityContextHolder.getContext().authentication
        val token = (authentication?.credentials as? String)?.takeIf { it.isNotBlank() }
            ?: run {
                log.warn("Token bo'sh yoki null, foydalanuvchi ID topilmadi.")
                return null
            }

        return try {
            val claims = jwtTokenUtil.decodeJwt(token)
            val userId = claims["id"]?.toString()?.toLongOrNull()
            if (userId != null) {
                log.info("Foydalanuvchi ID: $userId muvaffaqiyatli olingan.")
            } else {
                log.warn("Token ichida foydalanuvchi ID mavjud emas.")
            }
            userId
        } catch (e: Exception) {
            log.error("Tokenni dekodlashda xatolik: ${e.message}", e)
            null
        }
    }

    fun getCurrentUsername(): String? {
        val authentication = SecurityContextHolder.getContext().authentication
        val username = authentication?.name
        if (username != null) {
            log.info("Foydalanuvchi nomi: $username")
        } else {
            log.warn("Foydalanuvchi nomi olinmadi.")
        }
        return username
    }
}