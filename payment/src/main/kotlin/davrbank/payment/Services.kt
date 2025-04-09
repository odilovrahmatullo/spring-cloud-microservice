package davrbank.payment

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.*
import javax.persistence.EntityManager
import javax.transaction.Transactional

@FeignClient(name = "user", configuration = [PublicTokenConfig::class])
interface UserService {
    @GetMapping("internal/exists/{id}")
    fun existBy(@PathVariable("id") id: Long): Boolean

    @GetMapping("internal/{id}")
    fun getOne(@PathVariable id: Long): UserResponse

    @PutMapping("internal/{userId}/reduce/{money}")
    fun reduceMoney(@PathVariable userId: Long, @PathVariable money: BigDecimal)
}

@FeignClient(name = "course")
interface CourseService {
    @GetMapping("internal/exists/{id}")
    fun existBy(@PathVariable("id") id: Long): Boolean

    @GetMapping("internal/one/{id}")
    fun getOne(@PathVariable id: Long): CourseResponse
}


interface PaymentService {
    fun create(request: PaymentCreateRequest)
    fun getAll(pageable: Pageable): Page<PaymentResponse>
    fun getOne(id: Long): PaymentResponse
    fun delete(id: Long)
    fun getPayment(id: Long): Payment
    fun mostSold(pageable: Pageable): PageImpl<TopSellingCourse>
    fun getCourseIds(userId: Long): List<Long>
    fun filter(page: Int, size: Int, filterDto: PaymentFilterDto): PageImpl<PaymentResponse>
}


@Service
class PaymentServiceImpl(
    private val courseService: CourseService,
    private val userService: UserService,
    private val paymentRepository: PaymentRepository,
    private val securityUtil: SecurityUtil,
    private val entityManager: EntityManager
) : PaymentService {
    @Transactional
    override fun create(request: PaymentCreateRequest) {
        request.run {
            val course = courseService.getOne(courseId)

            val user = userService.getOne(securityUtil.getCurrentUserId()!!)


            if (paymentRepository.existsByUserIdAndCourseId(user.id, course.id)) throw PaymentExistException()

            validatePayment(user.balance, course.price, money)

            paymentRepository.save(toEntity(user.id))

            userService.reduceMoney(user.id, money)
        }

    }

    override fun getAll(pageable: Pageable): Page<PaymentResponse> {
        val pages: Page<Payment> = paymentRepository.findAllNotDeleted(pageable)
        return pages.map {
            val user = userService.getOne(it.userId)
            val course = courseService.getOne(it.courseId)
            PaymentResponse.toResponse(user, course, it)
        }
    }

    override fun getOne(id: Long): PaymentResponse {
        val payment = getPayment(id)
        val user = userService.getOne(payment.userId)
        val course = courseService.getOne(payment.courseId)
        return PaymentResponse.toResponse(user, course, payment)
    }

    override fun delete(id: Long) {
        paymentRepository.trash(id) ?: throw PaymentNotFoundException()
    }

    override fun getPayment(id: Long): Payment =
        paymentRepository.findByIdAndDeletedFalse(id) ?: throw PaymentNotFoundException()


    override fun mostSold(pageable: Pageable): PageImpl<TopSellingCourse> {
        val sql = """
            Select p.course_id, COUNT(*) as sales_count
            From payment p
            Group by p.course_id
            Order by sales_count Desc
            """.trimIndent()

        val countSql = """
            Select count(*) From (
            Select p.course_id
            From payment p Group by p.course_id) As subquery
            """.trimIndent()

        val resultSet = entityManager
            .createNativeQuery(sql)
            .setFirstResult(pageable.offset.toInt())
            .setMaxResults(pageable.pageSize)
            .resultList as List<Array<Any>>

        val mapped = resultSet.map { row ->
            TopSellingCourse(
                courseId = (row[0] as Number).toLong(),
                count = (row[1] as Number).toLong()
            )
        }

        val total = (entityManager.createNativeQuery(countSql).singleResult as Number).toLong()

        return PageImpl(mapped, pageable, total)

    }

    override fun getCourseIds(userId: Long): List<Long> {
        return paymentRepository.findByUserIdAndDeletedFalse(userId).map { it.courseId }
    }

    override fun filter(page: Int, size: Int, filterDto: PaymentFilterDto): PageImpl<PaymentResponse> {
        filterDto.run {

            if (fromDate != null && toDate != null) {
                if (fromDate.isAfter(toDate)) throw InvalidDateException()
            }

            val condition: StringBuilder = StringBuilder()
            val params: MutableMap<String, Any> = mutableMapOf()

            fromDate?.let {
                val fromDateTime: LocalDateTime = LocalDateTime.of(fromDate, LocalTime.MIN)
                val fromDateConverted: Date = Date.from(fromDateTime.atZone(ZoneId.systemDefault()).toInstant())
                condition.append("and p.createdDate>=:from ")
                params.put("from", fromDateConverted)
            }

            toDate?.let {
                val toDateTime: LocalDateTime = LocalDateTime.of(toDate, LocalTime.MAX)
                val toDateConverted: Date = Date.from(toDateTime.atZone(ZoneId.systemDefault()).toInstant())
                condition.append("and p.createdDate<=:to ")
                params.put("to", toDateConverted)
            }

            val selectBuilder: StringBuilder = StringBuilder("From Payment as p where p.deleted = false ")
            selectBuilder.append(condition)
            val countBuilder: StringBuilder =
                StringBuilder("Select count(*) from Payment as p where p.deleted = false ")
            countBuilder.append(condition)

            val selectQuery = entityManager.createQuery(selectBuilder.toString(), Payment::class.java)
            selectQuery.setFirstResult(page * size)
            selectQuery.setMaxResults(size)

            val countQuery = entityManager.createQuery(countBuilder.toString())

            for ((key, value) in params) {
                countQuery.setParameter(key, value)
                selectQuery.setParameter(key, value)
            }

            val list = selectQuery.resultList.map {
                val user = userService.getOne(it.userId)
                val course = courseService.getOne(it.courseId)
                PaymentResponse.toResponse(user, course, it)
            }
            val total = countQuery.singleResult as Long

            return PageImpl(list, PageRequest.of(page, size), total)

        }

    }

    private fun validatePayment(userMoney: BigDecimal, coursePrice: BigDecimal, paidMoney: BigDecimal) {

        if (userMoney < paidMoney) throw NotEnoughMoneyException()

        if (coursePrice < paidMoney) throw BigMoneyException()

        if (paidMoney < coursePrice) throw LessMoneyException()

    }
}
