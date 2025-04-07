package davrbank.user

import org.hibernate.annotations.ColumnDefault
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.data.jpa.repository.Temporal
import java.math.BigDecimal
import java.util.*
import javax.persistence.*

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
class BaseEntity(
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
    var username: String,
    @Column(nullable = false)
    var password: String,
    @Enumerated(EnumType.STRING)
    var gender: Gender,
    var balance: BigDecimal,
    @Enumerated(EnumType.STRING)
    var role: Role
) : BaseEntity()


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