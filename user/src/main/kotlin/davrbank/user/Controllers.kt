package davrbank.user

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import javax.validation.Valid


@RestController
class UserController(private val userService: UserService) {
    @GetMapping("list")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    fun list(@Valid params: RequestParams, @RequestParam(required = false) gender: Gender?): Page<UserResponse> {
        return userService.getAll(PageRequest.of(params.page, params.size), params.search, gender)
    }

    @GetMapping("one/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @securityUtil.getCurrentUserId() == #id")
    fun getOne(@PathVariable("id") id: Long) = userService.getOne(id)

    @PutMapping("edit")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    fun update(@RequestBody @Valid request: UserUpdateRequest) {
        userService.update(request)
    }

    @DeleteMapping("delete/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    fun delete(@PathVariable("id") id: Long) = userService.delete(id)

    @PutMapping("pay")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    fun updateBalance(@RequestBody @Valid updateBalanceRequest: UpdateBalanceRequest) {
        userService.payMoneyToBalance(updateBalanceRequest.balance)
    }

    @GetMapping("all")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    fun getAll() = userService.getAllCourseOfUser()

}

@RestController
@RequestMapping("internal")
class UserInternalController(private val userService: UserService) {

    @GetMapping("{id}")
    fun getOne(@PathVariable id: Long) =
        userService.getOne(id)

    @GetMapping("exists/{id}")
    fun existById(@PathVariable id: Long) = userService.existById(id)


    @PutMapping("/{userId}/reduce/{money}")
    fun reduceMoney(@PathVariable userId: Long, @PathVariable money: BigDecimal) {
        userService.reduceMoney(userService.getUser(userId), money)
    }

}