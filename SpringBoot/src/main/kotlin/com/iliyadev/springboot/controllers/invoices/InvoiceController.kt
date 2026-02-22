package com.iliyadev.springboot.controllers.invoices

import com.iliyadev.springboot.config.JwtTokenUtils.JwtTokenUtils
import com.iliyadev.springboot.models.invoices.Invoice
import com.iliyadev.springboot.services.invoices.InvoiceService
import com.iliyadev.springboot.utils.ServiceResponse
import com.iliyadev.springboot.utils.UserUtil
import com.iliyadev.springboot.utils.exceptions.NotfoundExceptions
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/invoice")
class InvoiceController {

    @Autowired
    private lateinit var service: InvoiceService

    @Autowired
    private lateinit var jwtUtil: JwtTokenUtils

    @GetMapping("/user/{userId}")
    fun getAllByUserId(
        @PathVariable userId: Long,
        @RequestParam pageSize: Int,
        @RequestParam pageIndex: Int,
        request: HttpServletRequest
    ): ServiceResponse<Invoice> {
        return try {
            val currentUser = UserUtil.getCurrentUsername(jwtUtil, request)
            val data = service.getAllUserById(userId, pageIndex, pageSize, currentUser)
            ServiceResponse(data, HttpStatus.OK)
        } catch (e: NotfoundExceptions) {
            ServiceResponse(status = HttpStatus.NOT_FOUND, message = e.message!!)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message!!)
        }

    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long,request: HttpServletRequest): ServiceResponse<Invoice> {
        return try {
            val currentUser = UserUtil.getCurrentUsername(jwtUtil, request)
            val data = service.getById(id,currentUser) ?: throw NotfoundExceptions("data not found")
            ServiceResponse(listOf(data), HttpStatus.OK)
        } catch (e: NotfoundExceptions) {
            ServiceResponse(status = HttpStatus.NOT_FOUND, message = e.message!!)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message!!)
        }
    }

    @PostMapping("")
    fun addInvoice(@RequestBody invoice: Invoice,request: HttpServletRequest): ServiceResponse<Invoice> {
        return try {
            val currentUser = UserUtil.getCurrentUsername(jwtUtil,request)
            val data = service.insert(invoice,currentUser)
            ServiceResponse(listOf(data), HttpStatus.OK)
        } catch (e: NotfoundExceptions) {
            ServiceResponse(status = HttpStatus.NOT_FOUND, message = e.message!!)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message!!)
        }
    }



}