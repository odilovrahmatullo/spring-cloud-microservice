package davrbank.payment

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import javax.validation.Valid


@RestController
class PaymentController(
    private val paymentsService: PaymentService
) {

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_USER')")
    fun create(@RequestBody @Valid paymentCreateRequest: PaymentCreateRequest) =
        paymentsService.create(paymentCreateRequest)

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    fun list(@Valid params: RequestParams): Page<PaymentResponse> {
        return paymentsService.getAll(PageRequest.of(params.page, params.size))
    }

    @GetMapping("{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    fun getOne(@PathVariable("id") id: Long): PaymentResponse {
        val payment = paymentsService.getOne(id)

        return payment

    }
    @GetMapping("filter")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    fun getFilterList(@Valid requestParams: RequestParams,@RequestBody filterDto: PaymentFilterDto):PageImpl<PaymentResponse>{
        return paymentsService.filter(requestParams.page,requestParams.size,filterDto)
    }

    @DeleteMapping("{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    fun delete(@PathVariable("id") id: Long) = paymentsService.delete(id)

}

@RestController
@RequestMapping("internal")
class PaymentInternalController(
    private val paymentsService: PaymentService
){
    @GetMapping("top-selling")
    fun getTopSelling(@RequestParam page:Int, @RequestParam size:Int):PageImpl<TopSellingCourse>{
        return paymentsService.mostSold(PageRequest.of(page,size))
    }
    @GetMapping("{userId}")
    fun getCourseByUserId(@PathVariable userId:Long):List<Long> = paymentsService.getCourseIds(userId)
}


