package com.iliyadev.springboot.viewmodels

import com.iliyadev.springboot.models.customers.User
import com.iliyadev.springboot.models.enums.UserType

data class UserViewModel(
    var id: Long = 0,
    val username: String = "",
    val oldPassword: String = "",
    var password: String = "",
    var repeatPassword: String = "",
    var token: String = "",
    var isAdmin: Boolean = false,
    
    // اطلاعات جدید برای اپلیکیشن کاریابی
    var userType: UserType = UserType.JOBSEEKER,
    var email: String? = null,
    var phone: String? = null,
    var fullName: String? = null,
    var profileImage: String? = null,
    var bio: String? = null,
    
    // اطلاعات کارفرما
    var companyName: String? = null,
    var companyLogo: String? = null,
    var companyDescription: String? = null,
    var companyWebsite: String? = null,
    var companySize: String? = null,
    
    // اطلاعات کارجو
    var skills: String? = null,
    var experience: String? = null,
    var education: String? = null
) {
    constructor(user: User) : this(
        id = user.id,
        username = user.username,
        isAdmin = user.isAdmin,
        userType = user.userType,
        email = user.email,
        phone = user.phone,
        fullName = user.fullName,
        profileImage = user.profileImage,
        bio = user.bio,
        companyName = user.companyName,
        companyLogo = user.companyLogo,
        companyDescription = user.companyDescription,
        companyWebsite = user.companyWebsite,
        companySize = user.companySize,
        skills = user.skills,
        experience = user.experience,
        education = user.education
    )

    fun convertToUser(): User {
        return User(
            id = id,
            username = username,
            password = password,
            isAdmin = isAdmin,
            userType = userType,
            email = email,
            phone = phone,
            fullName = fullName,
            profileImage = profileImage,
            bio = bio,
            companyName = companyName,
            companyLogo = companyLogo,
            companyDescription = companyDescription,
            companyWebsite = companyWebsite,
            companySize = companySize,
            skills = skills,
            experience = experience,
            education = education,
            createdAt = java.time.LocalDateTime.now().toString()
        )
    }
}