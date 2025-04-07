package davrbank.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver
import java.util.*

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
class SpringSecurityUtil {
    private fun getAuthentication(): Authentication {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication == null || !authentication.isAuthenticated || authentication.principal == "anonymousUser") {
            throw ForbiddenException()
        }
        return authentication
    }

    fun getCurrentUser(): UserDetails {
        return getAuthentication().principal as UserDetails
    }

    fun getCurrentUserId(): Long {
        return (getAuthentication().principal as User).getId()
    }
}
