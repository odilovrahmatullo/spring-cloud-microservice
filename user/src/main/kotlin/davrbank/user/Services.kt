package davrbank.user

import lombok.extern.slf4j.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import java.math.BigDecimal
import javax.transaction.Transactional

@FeignClient(name = "payment")
interface PaymentService {
    @GetMapping("/internal/{userId}")
    fun getCourseByUserId(@PathVariable userId: Long): List<Long>
}

@FeignClient(name = "course")
interface CourseService {
    @GetMapping("/internal/{ids}")
    fun getCourses(@PathVariable ids: String): List<CourseDto>
}

interface UserService {
    fun create(request: UserCreateRequest)
    fun getAll(pageable: Pageable, search: String, gender: Gender?): Page<UserResponse>
    fun getOne(id: Long): UserResponse
    fun delete(id: Long)
    fun getUser(id: Long): User
    fun update(request: UserUpdateRequest)
    fun existById(id: Long): Boolean
    fun payMoneyToBalance(money: BigDecimal)
    fun reduceMoney(user: User, money: BigDecimal)
    fun getAllCourseOfUser(): UserCourseDetails

}

@Service
@Slf4j
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val securityUtil: SecurityUtil,
    private val paymentService: PaymentService,
    private val courseService: CourseService
) : UserService {

    val log: Logger = LoggerFactory.getLogger(UserServiceImpl::class.java)

    override fun create(request: UserCreateRequest) {
        request.run {
            if (userRepository.existsByUsername(username)) throw UsernameAlreadyExistException()
            userRepository.save(toEntity(password))
        }
    }

    override fun getAll(pageable: Pageable, search: String, gender: Gender?): Page<UserResponse> {
        val pages = userRepository.getUsers(pageable, search, gender)
        return pages.map {
            UserResponse.toResponse(it)
        }
    }

    override fun getOne(id: Long): UserResponse {
        return UserResponse.toResponse(getUser(id))
    }

    @Transactional
    override fun delete(id: Long) {
        userRepository.trash(id) ?: throw UserNotFoundException()
    }

    override fun getUser(id: Long): User =
        userRepository.findByIdAndDeletedFalse(id) ?: throw UserNotFoundException()

    @Transactional
    override fun update(request: UserUpdateRequest) {
        val id = securityUtil.getCurrentUserId()!!
        val patient = getUser(id)
        request.run {
            fullName?.let {
                patient.fullName = it
            }
            username?.let {
                if (userRepository.existsByUsernameAndIdNot(username, id))
                    throw UsernameAlreadyExistException()
                patient.username = it
            }
        }
        userRepository.save(patient)
    }

    override fun existById(id: Long): Boolean {
        return userRepository.existsByIdAndDeletedFalse(id)
    }

    @Transactional
    override fun payMoneyToBalance(money: BigDecimal) {
        val userId = securityUtil.getCurrentUserId()!!
        log.info("payment ichiga kirdi => $userId")

        val user = getUser(userId)

        log.info("userni oldi $user")

        user.balance += money;

        userRepository.save(user)

        log.info("save qildi method tugadi")
    }

    @Transactional
    override fun reduceMoney(user: User, money: BigDecimal) {
        user.balance -= money

        userRepository.save(user)

    }


    @Transactional
    override fun getAllCourseOfUser(): UserCourseDetails {

        val user = getUser(securityUtil.getCurrentUserId()!!)

        val coursesIds = paymentService.getCourseByUserId(user.id!!)

        var courseDtoS: MutableList<CourseDto> = ArrayList()

        var totalPayment = BigDecimal.ZERO

        if (coursesIds.isNotEmpty()) {

            val coursesIdsStr = coursesIds.joinToString(",")

            courseDtoS = courseService.getCourses(coursesIdsStr) as MutableList<CourseDto>

            totalPayment = courseDtoS.sumOf { it.price }
        }

        return UserCourseDetails(userId = user.id!!, user.fullName, courseDtoS, totalPayment)

    }


}