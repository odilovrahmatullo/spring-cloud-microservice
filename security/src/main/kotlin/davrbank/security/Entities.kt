package davrbank.security


import org.hibernate.annotations.ColumnDefault
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.data.jpa.repository.Temporal
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.math.BigDecimal
import java.util.*
import javax.persistence.*

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
open class BaseEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @CreatedDate @Temporal(TemporalType.TIMESTAMP) var createdDate: Date? = null,
    @LastModifiedDate @Temporal(TemporalType.TIMESTAMP) var modifiedDate: Date? = null,
    @Column(nullable = false) @ColumnDefault(value = "false") var deleted: Boolean = false
)

@Entity
@Table(name = "users")
class User(
    @Column(length = 128, nullable = false)
    var fullName: String,
    @Column(length = 20,unique = true, nullable = false)
    @get:JvmName("getUserUsername")
    var username: String,
    @Column(nullable = false)
    @get:JvmName("getUserPassword")
    var password: String,
    @Enumerated(EnumType.STRING)
    var gender: Gender,
    var balance: BigDecimal,
    @Enumerated(EnumType.STRING)
    val role: Role
) : BaseEntity(), UserDetails {
    override fun getAuthorities(): List<SimpleGrantedAuthority> {
        return listOf(SimpleGrantedAuthority("$role"))
    }

    override fun getPassword(): String {
        return password
    }

    override fun getUsername(): String {
        return username
    }

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true

    fun getId(): Long {
        return id!!
    }
}

    @Embeddable
class LocalizedString(
    var uz: String,
    var ru: String,
    var en: String,
) {
    @Transient
    fun localized(): String {
        return when (LocaleContextHolder.getLocale().language) {
            "uz" -> this.uz
            "en" -> this.en
            "ru" -> this.ru
            else -> this.uz
        }
    }
}

@Entity
class RefreshToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne
    val user: User,
    val refreshToken: String,
    val deleted: Boolean = false
)