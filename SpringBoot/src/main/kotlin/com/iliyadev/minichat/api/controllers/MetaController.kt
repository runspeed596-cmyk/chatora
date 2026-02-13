package com.iliyadev.minichat.api.controllers

import com.iliyadev.minichat.core.response.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/meta")
class MetaController {

    @GetMapping("/countries")
    fun getCountries(): ApiResponse<List<CountryDto>> {
        // Phase 1: Static list. Phase 3: Load from DB or JSON.
        val list = listOf(
            CountryDto("US", "United States", "🇺🇸"),
            CountryDto("IR", "Iran", "🇮🇷"),
            CountryDto("DE", "Germany", "🇩🇪"),
            CountryDto("TR", "Turkey", "🇹🇷")
        )
        return ApiResponse.success(list)
    }

    @GetMapping("/languages")
    fun getLanguages(): ApiResponse<List<LanguageDto>> {
        val list = listOf(
            LanguageDto("en", "English"),
            LanguageDto("fa", "Persian"),
            LanguageDto("de", "German"),
            LanguageDto("tr", "Turkish")
        )
        return ApiResponse.success(list)
    }
}

data class CountryDto(val code: String, val name: String, val flag: String)
data class LanguageDto(val code: String, val name: String)
