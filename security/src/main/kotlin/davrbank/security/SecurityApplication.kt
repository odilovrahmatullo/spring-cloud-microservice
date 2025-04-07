package davrbank.security

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableFeignClients
@EnableJpaRepositories(repositoryBaseClass = BaseRepositoryImpl::class)
@EnableJpaAuditing
class SecurityApplication
fun main(args: Array<String>) {
    runApplication<SecurityApplication>(*args)
}
