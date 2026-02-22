package com.iliyadev.springboot.config

import com.iliyadev.springboot.models.customers.User
import com.iliyadev.springboot.models.enums.UserType
import com.iliyadev.springboot.models.jobs.CooperationType
import com.iliyadev.springboot.models.jobs.JobCategory
import com.iliyadev.springboot.models.locations.City
import com.iliyadev.springboot.models.locations.Province
import com.iliyadev.springboot.repositories.customers.UserRepository
import com.iliyadev.springboot.repositories.jobs.CooperationTypeRepository
import com.iliyadev.springboot.repositories.jobs.JobCategoryRepository
import com.iliyadev.springboot.repositories.locations.CityRepository
import com.iliyadev.springboot.repositories.locations.ProvinceRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class DataSeeder : CommandLineRunner {
    
    @Autowired
    private lateinit var categoryRepository: JobCategoryRepository
    
    @Autowired
    private lateinit var cooperationTypeRepository: CooperationTypeRepository
    
    @Autowired
    private lateinit var provinceRepository: ProvinceRepository
    
    @Autowired
    private lateinit var cityRepository: CityRepository
    
    @Autowired
    private lateinit var userRepository: UserRepository
    
    override fun run(vararg args: String?) {
        seedAdminUser()
        seedCategories()
        seedCooperationTypes()
        seedProvinces()
    }
    
    // ایجاد کاربر ادمین
    private fun seedAdminUser() {
        if (userRepository.findFirstByUsername("admin") != null) return
        
        userRepository.save(User(
            username = "admin",
            password = "admin",
            isAdmin = true,
            userType = UserType.JOBSEEKER,
            fullName = "مدیر سیستم",
            createdAt = java.time.LocalDateTime.now().toString()
        ))
        println("Admin user created: admin/admin")
    }
    
    private fun seedCategories() {
        if (categoryRepository.count() > 0) return
        
        val categories = listOf(
            "حسابداری / مالی",
            "مدیر مالی",
            "رئیس حسابداری",
            "کمک حسابدار",
            "حسابرس",
            "معامله‌گر و تحلیل‌گر بازارهای مالی",
            "منشی و مسئول دفتر",
            "امور اداری",
            "منابع انسانی",
            "فروش و بازاریابی",
            "کارشناس و مدیر فروش",
            "فروشنده / بازاریاب / صندوقدار",
            "نماینده علمی (Med Rep)",
            "پشتیبانی و ارتباط با مشتریان",
            "روابط عمومی",
            "دیجیتال مارکتینگ",
            "سئو",
            "تولید محتوا",
            "ترجمه",
            "برنامه‌نویسی و توسعه نرم‌افزار",
            "علوم داده / هوش مصنوعی",
            "شبکه و امنیت",
            "پشتیبانی سخت‌افزاری",
            "طراح بازی",
            "طراحی لباس / طلا و جواهر",
            "گرافیست / طراح",
            "موشن گرافیک",
            "طراحی صنعتی / نقشه‌کشی صنعتی",
            "عکاس",
            "طراحی رابط و تجربه کاربری (UI/UX)",
            "مهندس صنایع",
            "کنترل کیفیت",
            "برنامه‌ریزی و کنترل پروژه",
            "مدیر محصول / مالک محصول",
            "تحلیل کسب‌وکار / استراتژی",
            "تحلیل داده",
            "هوش تجاری",
            "مدیرعامل / مدیر کارخانه",
            "مدیر اجرایی / مدیر داخلی",
            "مدیر فروشگاه / مدیر رستوران",
            "بازرگانی و تجارت",
            "خرید و تدارکات",
            "انبارداری / حمل و نقل",
            "راننده / پیک موتوری",
            "نگهبانی و حراست",
            "مهندسی عمران",
            "مهندسی معماری و شهرسازی",
            "مهندسی پزشکی",
            "مهندسی انرژی",
            "مهندسی مکانیک / هوا فضا",
            "مهندسی برق",
            "مهندسی صنایع غذایی",
            "مهندسی نفت و گاز",
            "محیط زیست، ایمنی و امنیت (HSE)",
            "مهندسی مواد و متالورژی",
            "مهندسی معدن",
            "مهندسی پلیمر",
            "وکیل / حقوقی",
            "تحصیلدار و کارپرداز",
            "مدیریت بیمه",
            "کارگر ساده / نیروی خدماتی",
            "داروسازی و شیمی دارویی",
            "زیست‌شناسی و علوم آزمایشگاهی",
            "پزشک / دندانپزشک / دامپزشک",
            "پرستار / کادر سلامت و درمان",
            "روان‌شناسی و مشاوره",
            "تدریس"
        )
        
        categories.forEach { name ->
            categoryRepository.save(JobCategory(name = name))
        }
    }
    
    private fun seedCooperationTypes() {
        if (cooperationTypeRepository.count() > 0) return
        
        val types = listOf(
            "تمام وقت",
            "پاره وقت",
            "پروژه‌ای",
            "کارآموزی",
            "دورکاری"
        )
        
        types.forEach { name ->
            cooperationTypeRepository.save(CooperationType(name = name))
        }
    }
    
    private fun seedProvinces() {
        if (provinceRepository.count() > 0) return
        
        val provincesWithCities = mapOf(
            "تهران" to listOf("تهران", "شهریار", "کرج", "ورامین", "اسلامشهر", "ملارد"),
            "اصفهان" to listOf("اصفهان", "کاشان", "نجف‌آباد", "خمینی‌شهر"),
            "البرز" to listOf("کرج", "فردیس", "نظرآباد"),
            "خراسان رضوی" to listOf("مشهد", "نیشابور", "سبزوار", "تربت حیدریه"),
            "قزوین" to listOf("قزوین", "تاکستان", "آبیک"),
            "فارس" to listOf("شیراز", "مرودشت", "کازرون", "جهرم"),
            "خوزستان" to listOf("اهواز", "آبادان", "خرمشهر", "دزفول"),
            "گیلان" to listOf("رشت", "لاهیجان", "بندر انزلی", "آستارا"),
            "مازندران" to listOf("ساری", "بابل", "آمل", "قائم‌شهر"),
            "مرکزی" to listOf("اراک", "ساوه", "خمین"),
            "آذربایجان شرقی" to listOf("تبریز", "مراغه", "مرند"),
            "قم" to listOf("قم"),
            "کرمان" to listOf("کرمان", "رفسنجان", "جیرفت"),
            "یزد" to listOf("یزد", "میبد", "اردکان"),
            "سمنان" to listOf("سمنان", "شاهرود", "دامغان"),
            "هرمزگان" to listOf("بندرعباس", "قشم", "کیش"),
            "آذربایجان غربی" to listOf("ارومیه", "خوی", "مهاباد"),
            "کرمانشاه" to listOf("کرمانشاه", "اسلام‌آباد غرب"),
            "همدان" to listOf("همدان", "ملایر", "نهاوند"),
            "بوشهر" to listOf("بوشهر", "دشتستان"),
            "سیستان و بلوچستان" to listOf("زاهدان", "چابهار", "زابل"),
            "زنجان" to listOf("زنجان", "ابهر"),
            "گلستان" to listOf("گرگان", "گنبد کاووس"),
            "اردبیل" to listOf("اردبیل", "مشکین‌شهر"),
            "چهارمحال و بختیاری" to listOf("شهرکرد", "بروجن"),
            "لرستان" to listOf("خرم‌آباد", "بروجرد", "دورود"),
            "خراسان جنوبی" to listOf("بیرجند", "قائن"),
            "کردستان" to listOf("سنندج", "سقز", "مریوان"),
            "ایلام" to listOf("ایلام", "دهلران"),
            "خراسان شمالی" to listOf("بجنورد", "شیروان"),
            "کهگیلویه و بویراحمد" to listOf("یاسوج", "گچساران"),
            "خارج کشور" to listOf("دبی", "استانبول", "آنکارا", "لندن")
        )
        
        provincesWithCities.forEach { (provinceName, cities) ->
            val province = provinceRepository.save(Province(name = provinceName))
            cities.forEach { cityName ->
                cityRepository.save(City(name = cityName, province = province))
            }
        }
    }
}
