package davrbank.course

import lombok.extern.slf4j.Slf4j
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import javax.persistence.EntityManager
import javax.transaction.Transactional

@FeignClient(name = "payment", configuration = [FeignConfiguration::class])
interface PaymentService {
    @GetMapping("internal/top-selling")
    fun getTopSelling(@RequestParam page: Int, @RequestParam size: Int): PageImpl<TopCourse>
}

interface CourseService {
    fun create(request: CourseDto)
    fun getAll(pageable: Pageable, search: String): Page<CourseDto>
    fun getOne(id: Long): CourseDto
    fun getCoursesForInternal(coursesIds: List<Long>): List<CourseDto>
    fun delete(id: Long)
    fun getCourse(id: Long): Course
    fun update(id: Long, request: CourseUpdateRequest)
    fun existById(id: Long): Boolean
    fun mostSoldCourse(page: Int, size: Int): PageImpl<TopSellingDetailResponse>
    fun mostViewed(pageable: Pageable): Page<CourseDto>
    fun getOneForInternal(id: Long): CourseDto
}

@Service
@Slf4j
class CourseServiceImpl(
    private val courseRepository: CourseRepository,
    private val securityUtil: SecurityUtil,
    private val viewService: ViewService,
    private val viewRepository: ViewCountRepository,
    private val paymentService: PaymentService,
    private val entityManager: EntityManager
) : CourseService {

    override fun create(request: CourseDto) {
        request.run {
            if (courseRepository.existsByName(name)) throw CourseNameAlreadyExistException()
            courseRepository.save(toEntity())
        }
    }

    override fun getAll(pageable: Pageable, search: String): Page<CourseDto> {
        val pages = courseRepository.getCourses(pageable, search)
        return pages.map {
            val viewCount = viewRepository.getCourseViewCount(it.id!!)
            CourseDto.toResponse(it, viewCount)
        }
    }


    @Transactional
    override fun getOne(id: Long): CourseDto {
        val course = getCourse(id)

        val viewCount = viewService.updateViewCount(course, securityUtil.getCurrentUserId()!!)

        return CourseDto.toResponse(course, viewCount)
    }

    @Transactional
    override fun getOneForInternal(id: Long): CourseDto {
        val course = getCourse(id)

        val viewCount = viewRepository.getCourseViewCount(id)

        return CourseDto.toResponse(course, viewCount)
    }

    override fun getCoursesForInternal(coursesIds: List<Long>): List<CourseDto> {

        val courses = courseRepository.findAllByIdInAndDeletedFalse(coursesIds)

        if (courses.size != coursesIds.size) {
            throw CourseNotFoundExceptionInList()
        }

        val dtoS: MutableList<CourseDto> = ArrayList()
        courses.map {
            val viewCount = viewRepository.getCourseViewCount(it.id!!)
            val courseDto = CourseDto.toResponse(it, viewCount)
            dtoS.add(courseDto)
        }

        return dtoS
    }


    @Transactional
    override fun delete(id: Long) {
        courseRepository.trash(id) ?: throw CourseNotFoundException()
    }

    override fun getCourse(id: Long): Course =
        courseRepository.findByIdAndDeletedFalse(id) ?: throw CourseNotFoundException()

    @Transactional
    override fun update(id: Long, request: CourseUpdateRequest) {
        val course = getCourse(id)
        request.run {
            description?.let {
                course.description = it
            }
            name?.let {
                if (courseRepository.existsByNameAndIdNot(it, id))
                    throw CourseNameAlreadyExistException()
                course.name = it
            }
            price?.let {
                course.price = it
            }
        }
        courseRepository.save(course)
    }

    override fun existById(id: Long): Boolean {
        return courseRepository.existsByIdAndDeletedFalse(id)
    }

    @Transactional
    override fun mostSoldCourse(page: Int, size: Int): PageImpl<TopSellingDetailResponse> {
        val pages: PageImpl<TopCourse> = paymentService.getTopSelling(page, size)
        val result = pages.map {

            val viewCount = viewRepository.getCourseViewCount(it.courseId)

            val dto = CourseDto.toResponse(getCourse(it.courseId), viewCount)
            TopSellingDetailResponse(dto, soldCount = it.count)
        }
        return result as PageImpl<TopSellingDetailResponse>
    }

    override fun mostViewed(pageable: Pageable): PageImpl<CourseDto> {
        val sql = """
        SELECT c.id AS course_id, COUNT(v.id) AS views
        FROM course c
        LEFT JOIN view_count v ON c.id = v.course_id
        GROUP BY c.id
        ORDER BY views DESC
        LIMIT ? OFFSET ?;
    """.trimIndent()

        val resultSet = entityManager
            .createNativeQuery(sql)
            .setParameter(1, pageable.pageSize)  // LIMIT
            .setParameter(2, pageable.offset.toInt())  // OFFSET
            .resultList as List<Array<Any>>

        val mapped = resultSet.map { row ->
            val courseId = (row[0] as Number).toLong()
            val viewCount = (row[1] as Number).toLong()
            CourseDto.toResponse(getCourse(courseId), viewCount)
        }

        val totalQuery = """
         SELECT COUNT(DISTINCT c.id)
         FROM course c
         LEFT JOIN view_count v ON c.id = v.course_id """.trimIndent()


        val totalElements = (entityManager.createNativeQuery(totalQuery).singleResult as Number).toLong()

        return PageImpl(mapped, pageable, totalElements)

    }

}

interface ViewService {
    fun updateViewCount(course: Course, userId: Long): Long
}

@Service
class ViewServiceImpl(
    private val viewRepository: ViewCountRepository
) : ViewService {
    override fun updateViewCount(course: Course, userId: Long): Long {
        if (!viewRepository.existsByUserIdAndCourseId(userId, course.id!!)) {
            viewRepository.save(ViewCount(courseId = course.id!!, userId = userId))
        }
        return viewRepository.getCourseViewCount(course.id!!)

    }

}