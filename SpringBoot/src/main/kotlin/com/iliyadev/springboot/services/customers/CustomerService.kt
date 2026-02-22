package com.iliyadev.springboot.services.customers

import com.iliyadev.springboot.models.customers.Customer
import com.iliyadev.springboot.repositories.customers.CustomerRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CustomerService {
    @Autowired
    private lateinit var repository: CustomerRepository

    fun getById(id: Long): Customer? {
        val data = repository.findById(id)
        if(data.isEmpty) return null
        return data.get()
    }

    fun insert(data: Customer): Customer {
        if (data == null)
            throw Exception("Customer is Empty")
        if (data!!.firstName.isEmpty())
            throw Exception("First name is Empty")
        if (data!!.lastName.isEmpty())
            throw Exception("Last name is Empty")
        if (data!!.phone.isEmpty()) {
            throw Exception("Phone is Empty")
        }
        return repository.save(data)
    }

    fun update(data: Customer): Customer? {
        val oldData = getById(data.id) ?: return null
        oldData.address = data.address
        oldData.firstName = data.firstName
        oldData.lastName = data.lastName
        oldData.phone = data.phone
        oldData.postalCode = data.postalCode
        return repository.save(oldData)
    }
}