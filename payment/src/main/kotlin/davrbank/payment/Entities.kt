package davrbank.payment

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
class Payment(
    val courseId:Long,
    val userId:Long,
    val paidMoney:BigDecimal,
    @Enumerated(EnumType.STRING)
    val paymentStatus: PaymentStatus
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