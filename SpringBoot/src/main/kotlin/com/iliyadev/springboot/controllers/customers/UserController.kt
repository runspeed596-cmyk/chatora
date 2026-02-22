package com.iliyadev.springboot.controllers.customers

import com.iliyadev.springboot.config.JwtTokenUtils.JwtTokenUtils
import com.iliyadev.springboot.models.customers.User
import com.iliyadev.springboot.services.customers.UserService
import com.iliyadev.springboot.utils.ServiceResponse
import com.iliyadev.springboot.utils.UserUtil.Companion.getCurrentUsername
import com.iliyadev.springboot.utils.exceptions.NotfoundExceptions
import com.iliyadev.springboot.viewmodels.UserViewModel
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/user")
class UserController {

    @Autowired
    private lateinit var service: UserService

    @Autowired
    private lateinit var jwtUtil: JwtTokenUtils

    @GetMapping("/{id}")
    fun getById(request: HttpServletRequest): ServiceResponse<User> {
        return try {
            val currentUser = getCurrentUsername(jwtUtil,request)
            val data = service.getByUsername(currentUser) ?: throw NotfoundExceptions("data not found")
            ServiceResponse(listOf(data), HttpStatus.OK)
        } catch (e: NotfoundExceptions) {
            ServiceResponse(status = HttpStatus.NOT_FOUND, message = e.message!!)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message!!)
        }
    }

    @PostMapping("/login")
    fun login(@RequestBody user: UserViewModel): ServiceResponse<UserViewModel> {
        return try {
            val data = service.getUserByUserAndPas(user.username,user.password) ?: throw NotfoundExceptions("data not found")
            val vm = UserViewModel(data)
            vm.token = jwtUtil.generateToken(vm)!!
            ServiceResponse(listOf(vm), HttpStatus.OK)
        } catch (e: NotfoundExceptions) {
            ServiceResponse(status = HttpStatus.NOT_FOUND, message = e.message!!)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message!!)
        }
    }


    @PostMapping("/register")
    fun addUser(@RequestBody user: UserViewModel): ServiceResponse<UserViewModel> {
        return try {
            val data = service.insert(user.convertToUser())
            val vm = UserViewModel(data)
            vm.token = jwtUtil.generateToken(vm)!!
            ServiceResponse(listOf(vm), HttpStatus.OK)
        } catch (e: NotfoundExceptions) {
            ServiceResponse(status = HttpStatus.NOT_FOUND, message = e.message!!)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message!!)
        }
    }

    @PutMapping("/update")
    fun editUser(@RequestBody user: UserViewModel,request: HttpServletRequest): ServiceResponse<User?> {
        return try {
            val currentUser = getCurrentUsername(jwtUtil,request)
            val data = service.update(user.convertToUser(),currentUser)?: throw NotfoundExceptions("data not found")
            ServiceResponse(listOf(data), HttpStatus.OK)
        } catch (e: NotfoundExceptions) {
            ServiceResponse(status = HttpStatus.NOT_FOUND, message = e.message!!)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message!!)
        }
    }

    @PutMapping("/changePassword")
    fun changePassword(@RequestBody user: UserViewModel,request: HttpServletRequest): ServiceResponse<User?> {
        val currentUser = getCurrentUsername(jwtUtil,request)
        return try {
            val data = service.changePassword(user.convertToUser(),user.oldPassword,user.repeatPassword,currentUser)
            ServiceResponse(listOf(data), HttpStatus.OK)
        } catch (e: NotfoundExceptions) {
            ServiceResponse(status = HttpStatus.NOT_FOUND, message = e.message!!)
        } catch (e: Exception) {
            ServiceResponse(status = HttpStatus.INTERNAL_SERVER_ERROR, message = e.message!!)
        }
    }



}