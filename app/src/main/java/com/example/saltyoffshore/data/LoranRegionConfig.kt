package com.example.saltyoffshore.data

/**
 * Configuration for LORAN region with Mapbox source details.
 * Matches iOS LoranRegionConfig struct.
 */
data class LoranRegionConfig(
    val id: String,
    val name: String,
    val chainId: String,
    val mapboxSourceId: String,
    val sourceLayerName: String,
    val isActive: Boolean
) {
    /** Station pair for this region based on grid configuration */
    val stationPair: List<String>
        get() = when (id) {
            "9960wy" -> listOf("W", "Y")
            "9960xy" -> listOf("X", "Y")
            "7980yz" -> listOf("Y", "Z")
            "7980wz" -> listOf("W", "Z")
            else -> listOf("X", "Y")
        }

    companion object {
        /** All available LORAN regions */
        val allRegions: List<LoranRegionConfig> = listOf(
            LoranRegionConfig(
                id = "9960xy",
                name = "NC VA NY (9960 X-Y)",
                chainId = "9960",
                mapboxSourceId = "mapbox://snowcast.7m9v9vb4",
                sourceLayerName = "loran_grid_9960xy_ripcharts-4f0bdp",
                isActive = true
            ),
            LoranRegionConfig(
                id = "9960wy",
                name = "New England (9960 W-Y)",
                chainId = "9960",
                mapboxSourceId = "mapbox://snowcast.020r104h",
                sourceLayerName = "loran_grid_9960wy_ripcharts-2ln14a",
                isActive = false
            ),
            LoranRegionConfig(
                id = "7980yz",
                name = "FL GA SC (7980 Y-Z)",
                chainId = "7980",
                mapboxSourceId = "mapbox://snowcast.4unax9s7",
                sourceLayerName = "loran_grid_7980yz_ripcharts-aqg34r",
                isActive = false
            ),
            LoranRegionConfig(
                id = "7980wz",
                name = "South FL (7980 W-Z)",
                chainId = "7980",
                mapboxSourceId = "mapbox://snowcast.loran_grid_7980wz_ripcharts-5ln17d",
                sourceLayerName = "loran_grid_7980wz_ripcharts-5ln17d",
                isActive = false
            )
        )

        /** Only active regions (shown in UI) */
        val availableRegions: List<LoranRegionConfig>
            get() = allRegions.filter { it.isActive }

        /** Default region for new users */
        val default: LoranRegionConfig
            get() = allRegions.first { it.id == "9960xy" }

        /** Get region by ID */
        fun getRegion(id: String): LoranRegionConfig? =
            allRegions.find { it.id == id }

        /** Get regions by chain ID */
        fun getRegions(chainId: String): List<LoranRegionConfig> =
            allRegions.filter { it.chainId == chainId }
    }
}
