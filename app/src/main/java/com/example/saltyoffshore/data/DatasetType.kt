package com.example.saltyoffshore.data

/**
 * Dataset types matching iOS DatasetType enum exactly.
 * Provides metadata for rendering, filtering, and display.
 */
enum class DatasetType(val rawValue: String) {
    SST("sst"),
    CURRENTS("currents"),
    CHLOROPHYLL("chlorophyll"),
    EDDYS("eddys"),
    WATER_CLARITY("water_clarity"),
    SALINITY("salinity"),
    WATER_TYPE("water_type"),
    MLD("mld"),
    FSLE("fsle"),
    DISSOLVED_OXYGEN("dissolved_oxygen");

    val shortName: String
        get() = when (this) {
            SST -> "SST"
            CURRENTS -> "CUR"
            CHLOROPHYLL -> "CHL"
            EDDYS -> "SSH"
            WATER_CLARITY -> "KD"
            SALINITY -> "SAL"
            WATER_TYPE -> "TYPE"
            MLD -> "MLD"
            FSLE -> "FSLE"
            DISSOLVED_OXYGEN -> "O₂"
        }

    val numberDecimalPlaces: Int
        get() = when (this) {
            WATER_CLARITY -> 3
            CHLOROPHYLL, DISSOLVED_OXYGEN, FSLE, CURRENTS -> 2
            SALINITY, SST -> 1
            EDDYS, MLD, WATER_TYPE -> 0
        }

    /**
     * GeoJSON property for numeric values (crosshair, numbers layer).
     * Matches iOS DatasetFieldConfig.dataField.
     */
    val dataField: String
        get() = when (this) {
            SST -> "temperature"
            CURRENTS -> "speed"
            CHLOROPHYLL -> "concentration"
            EDDYS -> "sea_surface_height"
            WATER_CLARITY -> "Kd_490"
            SALINITY -> "salinity"
            WATER_TYPE -> "label"
            MLD -> "mixed_layer_depth"
            FSLE -> "fsle"
            DISSOLVED_OXYGEN -> "dissolved_oxygen"
        }

    val contourFieldName: String
        get() = when (this) {
            SST -> "temperature"
            CURRENTS -> "speed"
            CHLOROPHYLL -> "concentration"
            EDDYS -> "ssh"
            WATER_CLARITY -> "Kd_490"
            SALINITY -> "salinity"
            MLD -> "depth"
            FSLE -> "fsle"
            DISSOLVED_OXYGEN -> "dissolved_oxygen"
            WATER_TYPE -> "salinity"
        }

    val contourLabel: String
        get() = when (this) {
            SST -> "temp_label"
            CURRENTS -> "speed"
            CHLOROPHYLL -> "concentration"
            EDDYS -> "ssh"
            WATER_CLARITY -> "m^-1"
            SALINITY -> "salinity_label"
            MLD -> "depth_label"
            FSLE -> "fsle_label"
            DISSOLVED_OXYGEN -> "oxygen_label"
            WATER_TYPE -> "salinity_label"
        }

    val supportsFronts: Boolean
        get() = this in listOf(SST, EDDYS, SALINITY, MLD)

    /**
     * Default contour line color (Android Color int).
     */
    val contourColor: Int
        get() = when (this) {
            SST -> 0xFF000000.toInt()          // Black
            CURRENTS -> 0xFF1E90FF.toInt()     // Dodger blue
            CHLOROPHYLL -> 0xFF228B22.toInt()  // Forest green
            EDDYS -> 0xFF000000.toInt()        // Black
            WATER_CLARITY -> 0xFF4169E1.toInt()// Royal blue
            SALINITY -> 0xFF800080.toInt()     // Purple
            WATER_TYPE -> 0xFF808080.toInt()   // Gray
            MLD -> 0xFF000080.toInt()          // Navy
            FSLE -> 0xFFFF4500.toInt()         // Orange red
            DISSOLVED_OXYGEN -> 0xFF008B8B.toInt() // Dark cyan
        }

    /**
     * Default colorscale for this dataset type.
     * Returns the Colorscale object (not just the ID).
     */
    val defaultColorscale: Colorscale
        get() = when (this) {
            SST -> Colorscale.SST
            CURRENTS -> Colorscale.CURRENTS
            CHLOROPHYLL -> Colorscale.CHLOROPHYLL
            EDDYS -> Colorscale.RDBU
            WATER_CLARITY -> Colorscale.VIRIDIS
            SALINITY -> Colorscale.FLOW
            WATER_TYPE -> Colorscale.VIRIDIS
            MLD -> Colorscale.CASCADE
            FSLE -> Colorscale.SALTY_VIBES
            DISSOLVED_OXYGEN -> Colorscale.BOUNDARY_FIRE
        }

    /**
     * Default colorscale ID for TiTiler API.
     */
    val defaultColorscaleId: String
        get() = defaultColorscale.id

    val rangeKey: String
        get() = when (this) {
            SST -> "sea_surface_temperature"
            CURRENTS -> "speed"
            CHLOROPHYLL -> "chlor_a"
            EDDYS -> "sea_surface_height"
            WATER_CLARITY -> "Kd_490"
            SALINITY -> "sss"
            WATER_TYPE -> "water_type"
            MLD -> "mlotst"
            FSLE -> "fsle"
            DISSOLVED_OXYGEN -> "o2"
        }

    companion object {
        fun fromRawValue(value: String): DatasetType? {
            val mapped = if (value == "ssh") "eddys" else value
            return entries.find { it.rawValue == mapped }
        }
    }
}
