package com.example.saltyoffshore.data

/**
 * Dataset types matching iOS DatasetType enum exactly.
 * Provides metadata for rendering, filtering, and display.
 */
enum class DatasetType(val rawValue: String) {
    SST("sst"),
    CURRENTS("currents"),
    CHLOROPHYLL("chlorophyll"),
    SEA_SURFACE_HEIGHT("sea_surface_height"),
    WATER_CLARITY("water_clarity"),
    SALINITY("salinity"),
    WATER_TYPE("water_type"),
    MLD("mld"),
    FSLE("fsle"),
    DISSOLVED_OXYGEN("dissolved_oxygen"),
    PHYTOPLANKTON("phytoplankton");

    val displayName: String
        get() = when (this) {
            SST -> "Sea Surface Temperature"
            CURRENTS -> "Currents"
            CHLOROPHYLL -> "Chlorophyll"
            SEA_SURFACE_HEIGHT -> "Sea Surface Height"
            WATER_CLARITY -> "Water Clarity"
            SALINITY -> "Salinity"
            WATER_TYPE -> "Water Type"
            MLD -> "Mixed Layer Depth"
            FSLE -> "FSLE"
            DISSOLVED_OXYGEN -> "Dissolved Oxygen"
            PHYTOPLANKTON -> "Phytoplankton"
        }

    val shortName: String
        get() = when (this) {
            SST -> "SST"
            CURRENTS -> "CUR"
            CHLOROPHYLL -> "CHL"
            SEA_SURFACE_HEIGHT -> "SSH"
            WATER_CLARITY -> "KD"
            SALINITY -> "SAL"
            WATER_TYPE -> "TYPE"
            MLD -> "MLD"
            FSLE -> "FSLE"
            DISSOLVED_OXYGEN -> "O\u2082"
            PHYTOPLANKTON -> "PHY"
        }

    val numberDecimalPlaces: Int
        get() = when (this) {
            WATER_CLARITY -> 3
            CHLOROPHYLL, DISSOLVED_OXYGEN, FSLE, CURRENTS, PHYTOPLANKTON -> 2
            SALINITY, SST -> 1
            SEA_SURFACE_HEIGHT, MLD, WATER_TYPE -> 0
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
            SEA_SURFACE_HEIGHT -> "sea_surface_height"
            WATER_CLARITY -> "Kd_490"
            SALINITY -> "salinity"
            WATER_TYPE -> "label"
            MLD -> "mixed_layer_depth"
            FSLE -> "fsle"
            DISSOLVED_OXYGEN -> "dissolved_oxygen"
            PHYTOPLANKTON -> "phyc"
        }

    val contourFieldName: String
        get() = when (this) {
            SST -> "temperature"
            CURRENTS -> "speed"
            CHLOROPHYLL -> "concentration"
            SEA_SURFACE_HEIGHT -> "ssh"
            WATER_CLARITY -> "Kd_490"
            SALINITY -> "salinity"
            MLD -> "depth"
            FSLE -> "fsle"
            DISSOLVED_OXYGEN -> "dissolved_oxygen"
            WATER_TYPE -> "salinity"
            PHYTOPLANKTON -> "phyc"
        }

    val contourLabel: String
        get() = when (this) {
            SST -> "temp_label"
            CURRENTS -> "speed"
            CHLOROPHYLL -> "concentration"
            SEA_SURFACE_HEIGHT -> "ssh"
            WATER_CLARITY -> "m^-1"
            SALINITY -> "salinity_label"
            MLD -> "depth_label"
            FSLE -> "fsle_label"
            DISSOLVED_OXYGEN -> "oxygen_label"
            WATER_TYPE -> "salinity_label"
            PHYTOPLANKTON -> "phyc_label"
        }

    /** iOS: only SST has BreaksConfig (thermal fronts from sst_gradient_magnitude) */
    val supportsFronts: Boolean
        get() = this == SST

    /**
     * Default contour line color (Android Color int).
     * iOS: always .black — contour color is user-editable but defaults to black for all types.
     */
    val contourColor: Int
        get() = android.graphics.Color.BLACK

    /**
     * Default colorscale for this dataset type.
     * Returns the Colorscale object (not just the ID).
     */
    val defaultColorscale: Colorscale
        get() = when (this) {
            SST -> Colorscale.SST
            CURRENTS -> Colorscale.CURRENTS
            CHLOROPHYLL -> Colorscale.CHLOROPHYLL
            SEA_SURFACE_HEIGHT -> Colorscale.RDBU
            WATER_CLARITY -> Colorscale.VIRIDIS
            SALINITY -> Colorscale.FLOW
            WATER_TYPE -> Colorscale.VIRIDIS
            MLD -> Colorscale.CASCADE
            FSLE -> Colorscale.SALTY_VIBES
            DISSOLVED_OXYGEN -> Colorscale.BOUNDARY_FIRE
            PHYTOPLANKTON -> Colorscale.BLOOM // iOS: .bloom (not chlorophyll)
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
            SEA_SURFACE_HEIGHT -> "sea_surface_height"
            WATER_CLARITY -> "Kd_490"
            SALINITY -> "sss"
            WATER_TYPE -> "label"
            MLD -> "mlotst"
            FSLE -> "fsle"
            DISSOLVED_OXYGEN -> "o2"
            PHYTOPLANKTON -> "phyc"
        }

    companion object {
        val default: DatasetType = SST

        fun fromRawValue(value: String): DatasetType? {
            // Legacy aliases: "ssh" and "eddys" both map to sea_surface_height
            val mapped = when (value) {
                "ssh", "eddys" -> "sea_surface_height"
                else -> value
            }
            return entries.find { it.rawValue == mapped }
        }
    }
}

/**
 * Layer capabilities for this dataset type (single source of truth).
 * Matches iOS LayerCapabilities exactly.
 */
val DatasetType.capabilities: LayerCapabilities
    get() = when (this) {
        DatasetType.SST -> LayerCapabilities(
            hasVisualLayer = true,
            hasContours = true,
            hasNumbers = true,
            hasEntrySelection = true
        )
        DatasetType.CURRENTS -> LayerCapabilities(
            hasVisualLayer = true,
            hasArrows = true,
            hasParticles = true,
            hasNumbers = true
        )
        DatasetType.SEA_SURFACE_HEIGHT -> LayerCapabilities(
            hasVisualLayer = true,
            hasContours = true,
            hasSshFeatures = true,
            hasNumbers = true
        )
        DatasetType.SALINITY, DatasetType.MLD, DatasetType.FSLE -> LayerCapabilities(
            hasVisualLayer = true,
            hasContours = true,
            hasNumbers = true
        )
        DatasetType.DISSOLVED_OXYGEN -> LayerCapabilities(
            hasVisualLayer = true,
            hasContours = true,
            hasNumbers = true,
            hasDepthSelection = true
        )
        DatasetType.CHLOROPHYLL -> LayerCapabilities(
            hasVisualLayer = true,
            hasContours = true,
            hasNumbers = true
        )
        DatasetType.PHYTOPLANKTON -> LayerCapabilities(
            hasVisualLayer = true,
            hasContours = true,
            hasNumbers = true
        )
        DatasetType.WATER_CLARITY, DatasetType.WATER_TYPE -> LayerCapabilities(
            hasVisualLayer = true,
            hasNumbers = true
        )
    }

/**
 * Rendering defaults for this dataset type (layer toggles, not visual config).
 * Matches iOS DatasetType.defaults exactly.
 */
val DatasetType.defaults: DatasetDefaults
    get() = when (this) {
        DatasetType.SEA_SURFACE_HEIGHT -> DatasetDefaults(
            contourEnabled = true,
            contourOpacity = 1.0,
            visualOpacity = 1.0,
            arrowsEnabled = false,
            arrowsOpacity = 1.0,
            particlesEnabled = false,
            numbersEnabled = false,
            numbersOpacity = 1.0
        )
        DatasetType.CURRENTS -> DatasetDefaults(
            contourEnabled = false,
            contourOpacity = 1.0,
            visualOpacity = 1.0,
            arrowsEnabled = false,
            arrowsOpacity = 1.0,
            particlesEnabled = true,
            numbersEnabled = false,
            numbersOpacity = 1.0
        )
        else -> DatasetDefaults.standard
    }

/**
 * Dataset grouping for UI organization.
 * Matches iOS DatasetType.Group exactly.
 */
enum class DatasetGroup(val title: String, val displayOrder: Int) {
    TEMPERATURE("TEMPERATURE", 0),
    CHLOROPHYLL("CHLOROPHYLL", 1),
    CURRENTS("CURRENTS", 2),
    OTHER("OTHER", 3);
}

/**
 * Group assignment for each dataset type.
 * Matches iOS DatasetType.group exactly.
 */
val DatasetType.group: DatasetGroup
    get() = when (this) {
        DatasetType.SST -> DatasetGroup.TEMPERATURE
        DatasetType.CHLOROPHYLL, DatasetType.PHYTOPLANKTON -> DatasetGroup.CHLOROPHYLL
        DatasetType.CURRENTS -> DatasetGroup.CURRENTS
        DatasetType.SEA_SURFACE_HEIGHT, DatasetType.WATER_CLARITY, DatasetType.SALINITY,
        DatasetType.WATER_TYPE, DatasetType.MLD, DatasetType.FSLE, DatasetType.DISSOLVED_OXYGEN -> DatasetGroup.OTHER
    }

// scaleMode is defined in RenderingConfig.kt via renderingConfig.scaleMode

/**
 * Zarr variable name for this dataset type.
 * Used when loading Zarr data slices.
 */
val DatasetType.zarrVariable: String
    get() = when (this) {
        DatasetType.SST -> "sea_surface_temperature"
        DatasetType.CURRENTS -> "speed"
        DatasetType.CHLOROPHYLL -> "chlor_a"
        DatasetType.SEA_SURFACE_HEIGHT -> "sea_surface_height"
        DatasetType.WATER_CLARITY -> "Kd_490"
        DatasetType.SALINITY -> "sss"
        DatasetType.WATER_TYPE -> "label"
        DatasetType.MLD -> "mlotst"
        DatasetType.FSLE -> "fsle"
        DatasetType.DISSOLVED_OXYGEN -> "o2"
        DatasetType.PHYTOPLANKTON -> "phyc"
    }
