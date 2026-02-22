package com.iliyadev.springboot.services.customers

import com.iliyadev.springboot.models.customers.User
import com.iliyadev.springboot.repositories.customers.UserRepository
import com.iliyadev.springboot.utils.SecurityUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UserService {
    @Autowired
    lateinit var repository: UserRepository

    fun getById(id: Long): User? {
        val data = repository.findById(id)
        if (data.isEmpty) return null
        return data.get()
    }

    fun getUserByUserAndPas(user: String, pass: String): User? {
        if (user.isEmpty() || pass.isEmpty())
            throw Exception("please fill username and password")
        val password = SecurityUtil.encryptSHA256(pass)
        return repository.findFirstByUsernameAndPassword(user, password)
    }

    fun getByUsername(user: String): User? {
        if (user.isEmpty())
            throw Exception("please fill username and password")
        return repository.findFirstByUsername(user)
    }

    fun insert(data: User): User {
        if (data.username.isEmpty())
            throw Exception("Username is Empty")
        if (data.password.isEmpty())
            throw Exception("Password is Empty")
        val oldData = getByUsername(data.username)
        if(oldData != null)
            throw Exception("User already Registered With this Username")
        val password = SecurityUtil.encryptSHA256(data.password)
        data.password = password
        val saveData = repository.save(data)
        saveData.password = ""
        return saveData
    }

    fun update(data: User, currentUser: String): User? {
        val user = repository.findFirstByUsername(currentUser)
        if(user == null || user.id != data.id)
            throw Exception("You Dont Have Permission To Update Info")
        
        // به‌روزرسانی اطلاعات کاربر
        val updatedUser = user.copy(
            fullName = data.fullName,
            phone = data.phone,
            bio = data.bio,
            profileImage = data.profileImage,
            companyName = data.companyName,
            companyLogo = data.companyLogo,
            companyDescription = data.companyDescription,
            companyWebsite = data.companyWebsite,
            companySize = data.companySize,
            skills = data.skills,
            experience = data.experience,
            education = data.education
        )
        
        return repository.save(updatedUser)
    }

    fun changePassword(data: User, oldPassword: String, repeatPassword: String, currentUser: String): User? {
        val user = repository.findFirstByUsername(currentUser)
        if(user == null || user.id != data.id)
            throw Exception("You Dont Have Permission To Update Info")
        if (data.password != repeatPassword)
            throw Exception("Passwords not match")
        if(user.password != SecurityUtil.encryptSHA256(oldPassword))
            throw Exception("Passwords not match")
        user.password = SecurityUtil.encryptSHA256(data.password)
        val saveData = repository.save(user)
        saveData.password = ""
        return saveData
    }
}