package com.example.saltyoffshore.data

import com.example.saltyoffshore.config.AppConstants
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter

/**
 * Fetches and caches dataset guide content from the API.
 * iOS ref: Services/DatasetGuideService.swift + Services/DatasetGuideAPI.swift
 */
object DatasetGuideService {

    data class GuideContent(
        val introduction: String,
        val datasets: List<DatasetGuideInfo>
    )

    suspend fun fetchGuides(): GuideContent {
        val response: GuideResponse = SaltyApi.client.get("${AppConstants.apiBaseURL}/guides") {
            // Cache-busting
            parameter("t", System.currentTimeMillis())
            header("Cache-Control", "no-cache")
            header("Pragma", "no-cache")
        }.body()

        // Separate overview from dataset sections (matches iOS logic)
        val overviewItem = response.guides.firstOrNull { guide ->
            guide.dataType == "overview" || guide.slug == "overview"
        }

        val sectionItems = response.guides.filter { guide ->
            guide.dataType != "overview" && guide.slug != "overview"
        }

        return GuideContent(
            introduction = overviewItem?.longDescription ?: "",
            datasets = sectionItems.map { it.toInfo() }
        )
    }
}
