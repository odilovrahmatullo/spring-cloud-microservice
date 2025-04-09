package davrbank.course

import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.CreationTimestamp
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.data.jpa.repository.Temporal
import java.math.BigDecimal
import java.time.LocalDateTime
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
class Course(
    @Column(length = 64, nullable = false)
    var name: String,
    @Column(columnDefinition = "TEXT")
    var description: String,
    @Column(nullable = false)
    var price: BigDecimal,
    @Column(nullable = false)
    var expiredDate: Long
) : BaseEntity()

@Entity
class ViewCount(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id:Long?=null,

    val courseId:Long,

    val userId:Long,

    @CreationTimestamp
    val viewDate:LocalDateTime?=null
)

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