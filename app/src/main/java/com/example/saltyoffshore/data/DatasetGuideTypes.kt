package com.example.saltyoffshore.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// MARK: - API Response Models

@Serializable
data class GuideResponse(
    val guides: List<GuideItem>
)

@Serializable
data class GuideItem(
    val id: String,
    val slug: String,
    val title: String,
    @SerialName("short_description") val shortDescription: String,
    @SerialName("long_description") val longDescription: String,
    @SerialName("data_type") val dataType: String,
    @SerialName("image_urls") val imageUrls: List<String>
)

// MARK: - App Domain Models

data class DatasetGuideInfo(
    val title: String,
    val description: String,
    val imageURLs: List<String>,
    val datasetType: DatasetType?
)

// MARK: - Mapping

fun GuideItem.toInfo(): DatasetGuideInfo = DatasetGuideInfo(
    title = title,
    description = longDescription,
    imageURLs = imageUrls,
    datasetType = DatasetType.entries.find { it.rawValue == dataType }
)
