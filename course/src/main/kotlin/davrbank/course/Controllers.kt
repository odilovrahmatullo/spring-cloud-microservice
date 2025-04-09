package davrbank.course

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import javax.validation.Valid


@RestController
class CourseController(private val courseService: CourseService) {

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    fun create(@RequestBody @Valid request: CourseDto) = courseService.create(request)

    @GetMapping("list")
    fun list(@Valid params: RequestParams): Page<CourseDto> {
        return courseService.getAll(PageRequest.of(params.page, params.size), params.search)
    }

    @GetMapping("one/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_ADMIN')")
    fun getOne(@PathVariable("id") id: Long) = courseService.getOne(id)

    @PutMapping("{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    fun update(@PathVariable("id") id: Long, @RequestBody @Valid request: CourseUpdateRequest) =
        courseService.update(id, request)

    @DeleteMapping("{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    fun delete(@PathVariable("id") id: Long) = courseService.delete(id)


    @GetMapping("most-sold")
    fun getMostPopular(@Valid requestParams: RequestParams) =
        courseService.mostSoldCourse(requestParams.page, requestParams.size)

    @GetMapping("most-viewed")
    fun getMostViewed(@Valid requestParams: RequestParams) = courseService.mostViewed(PageRequest.of(requestParams.page, requestParams.size))


}


@RestController
@RequestMapping("/internal")
class CourseInternalController(private val courseService: CourseService) {
    @GetMapping("{ids}")
    fun getOneForInternal(@PathVariable ids: String) : List<CourseDto> {
        val courseIds = ids.split(",").map { it.toLong()}
        return courseService.getCoursesForInternal(courseIds)
    }

    @GetMapping("one/{id}")
    fun getOne(@PathVariable("id") id: Long) = courseService.getOneForInternal(id)


    @GetMapping("exists/{id}")
    fun existById(@PathVariable id: Long) = courseService.existById(id)
}