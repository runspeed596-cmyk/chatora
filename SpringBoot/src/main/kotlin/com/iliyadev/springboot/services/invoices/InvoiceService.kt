package com.iliyadev.springboot.services.invoices

import com.iliyadev.springboot.models.enums.InvoiceStatus
import com.iliyadev.springboot.models.invoices.Invoice
import com.iliyadev.springboot.repositories.invoices.InvoiceRepository
import com.iliyadev.springboot.services.InvoiceItems.InvoiceItemService
import com.iliyadev.springboot.services.customers.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.util.Calendar

@Service
class InvoiceService {
    @Autowired
    lateinit var repository: InvoiceRepository

    @Autowired
    private lateinit var invoiceItemService: InvoiceItemService

    @Autowired
    private lateinit var userService: UserService

    fun getById(id: Long,currentUser: String): Invoice? {
        val data = repository.findById(id)
        if (data.isEmpty) return null
        val user = userService.getByUsername(currentUser)
        if(user == null || user.id == data.get().user!!.id)
            throw Exception("you don't have permission to access this data")
        return data.get()
    }

    fun getAllUserById(userId: Long, pageIndex: Int, pageSize: Int,currentUser: String): List<Invoice> {
        val user = userService.getByUsername(currentUser)
        if(user == null || user.id == userId)
            throw Exception("you don't have permission to access this data")
        val pageRequest = PageRequest.of(pageIndex, pageSize, Sort.by("id"))
        return repository.findAllByUserId(userId, pageRequest).toList()
    }

    fun insert(data: Invoice,currentUser: String): Invoice {

        if (data.items!!.isEmpty())
            throw Exception("items is empty")
        if(data.user!!.id == null || data.user!!.id <= 0)
            throw Exception("user Id is invalid")
        val user = userService.getByUsername(currentUser)
        if (user == null || user.id != data.user!!.id)
            throw Exception("you don't have permission to access this data")
        data.status = InvoiceStatus.NotPayed
        val dt = Calendar.getInstance()
        data.addDate =
            "${dt.get(Calendar.YEAR)}-${dt.get(Calendar.MONTH)}-${dt.get(Calendar.DAY_OF_MONTH)} ${dt.get(Calendar.HOUR)}:${
                dt.get(Calendar.MINUTE)
            }:${dt.get(Calendar.SECOND)}"
        data.paymentDate = ""
        data.transactions = null
        data.items!!.forEach {
            invoiceItemService.addItem(it)
        }
        return repository.save(data)
    }

    fun update(data: Invoice,currentUser: String): Invoice? {
        val oldData = getById(data.id,currentUser) ?: return null
        oldData.paymentDate = data.paymentDate
        oldData.status = data.status
        return repository.save(oldData)
    }
}