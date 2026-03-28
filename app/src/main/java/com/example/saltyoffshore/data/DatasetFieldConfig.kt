package com.example.saltyoffshore.data

/**
 * Configuration for gradient/breaks layer visualization.
 */
data class BreaksConfig(
    /** GeoJSON property containing gradient data (e.g., "sst_gradient_magnitude") */
    val fieldName: String,
    /** COG band containing gradient data (e.g., "b2") */
    val bandName: String,
    /** Default colorscale for breaks visualization */
    val colorscale: Colorscale
)

/**
 * Simplified dataset field mapping - 5 core fields + 1 legacy exception.
 */
data class DatasetFieldConfig(
    /** API lookup key for ranges: `entry.ranges[rangeKey]` */
    val rangeKey: String,
    /** GeoJSON property for numeric values (crosshair, numbers layer) */
    val dataField: String,
    /** GeoJSON property for display text on contour lines */
    val contourLabel: String,
    /** Default display unit */
    val unit: DatasetUnit,
    /** Optional gradient layer config (null = not supported) */
    val breaks: BreaksConfig? = null,
    /** LEGACY: Backend inconsistency - some datasets use different field for contour filtering.
     * When null, consumers should use `dataField`. TODO: Standardize backend then remove. */
    private val contourFilterFieldOverride: String? = null
) {
    /** Field name for contour filtering/coloring. Returns override if set, otherwise dataField. */
    val contourFilterField: String get() = contourFilterFieldOverride ?: dataField

    /** Whether this dataset supports breaks/gradient visualization */
    val supportsBreaks: Boolean get() = breaks != null
}

// MARK: - DatasetType Extension Properties

/** Dataset field configuration - single source of truth for field mappings */
val DatasetType.fields: DatasetFieldConfig
    get() = when (this) {
        DatasetType.SST -> DatasetFieldConfig(
            rangeKey = "sea_surface_temperature",
            dataField = "temperature",
            contourLabel = "temp_label",
            unit = DatasetUnit.FAHRENHEIT,
            breaks = BreaksConfig(
                fieldName = "sst_gradient_magnitude",
                bandName = "b2",
                colorscale = Colorscale.MAGENTA
            )
        )
        DatasetType.CURRENTS -> DatasetFieldConfig(
            rangeKey = "speed",
            dataField = "speed",
            contourLabel = "speed",
            unit = DatasetUnit.KNOTS
        )
        DatasetType.CHLOROPHYLL -> DatasetFieldConfig(
            rangeKey = "chlor_a",
            dataField = "concentration",
            contourLabel = "concentration",
            unit = DatasetUnit.MG_PER_CUBIC_METER
        )
        DatasetType.SEA_SURFACE_HEIGHT -> DatasetFieldConfig(
            rangeKey = "sea_surface_height",
            dataField = "sea_surface_height",
            contourLabel = "ssh",
            unit = DatasetUnit.CENTIMETERS,
            contourFilterFieldOverride = "ssh"
        )
        DatasetType.WATER_CLARITY -> DatasetFieldConfig(
            rangeKey = "Kd_490",
            dataField = "Kd_490",
            contourLabel = "m^-1",
            unit = DatasetUnit.INVERSE_METERS
        )
        DatasetType.SALINITY -> DatasetFieldConfig(
            rangeKey = "sss",
            dataField = "salinity",
            contourLabel = "salinity_label",
            unit = DatasetUnit.PSU
        )
        DatasetType.WATER_TYPE -> DatasetFieldConfig(
            rangeKey = "label",
            dataField = "label",
            contourLabel = "salinity_label",
            unit = DatasetUnit.DIMENSIONLESS,
            contourFilterFieldOverride = "salinity"
        )
        DatasetType.MLD -> DatasetFieldConfig(
            rangeKey = "mlotst",
            dataField = "mixed_layer_depth",
            contourLabel = "depth_label",
            unit = DatasetUnit.METERS,
            contourFilterFieldOverride = "depth"
        )
        DatasetType.FSLE -> DatasetFieldConfig(
            rangeKey = "fsle",
            dataField = "fsle",
            contourLabel = "fsle_label",
            unit = DatasetUnit.INVERSE_SECONDS
        )
        DatasetType.DISSOLVED_OXYGEN -> DatasetFieldConfig(
            rangeKey = "o2",
            dataField = "dissolved_oxygen",
            contourLabel = "oxygen_label",
            unit = DatasetUnit.MG_PER_LITER
        )
        DatasetType.PHYTOPLANKTON -> DatasetFieldConfig(
            rangeKey = "phyc",
            dataField = "phyc",
            contourLabel = "phyc_label",
            unit = DatasetUnit.MMOL_PER_CUBIC_METER
        )
    }

/** Available variables for this dataset type.
 * Returns multiple variables for datasets with gradient support, single variable otherwise. */
val DatasetType.availableVariables: List<DatasetVariable>
    get() = when (this) {
        DatasetType.SST -> listOf(
            DatasetVariable(
                id = "sst",
                displayName = "Temperature",
                zarrVariableName = "sea_surface_temperature",
                rangeKey = "sea_surface_temperature",
                hasPMTilesData = true,
                unit = DatasetUnit.FAHRENHEIT
            ),
            DatasetVariable(
                id = "sst_gradient_magnitude",
                displayName = "Break",
                zarrVariableName = "sst_gradient_magnitude",
                rangeKey = "sst_gradient_magnitude",
                hasPMTilesData = false,
                unit = DatasetUnit.CELSIUS_PER_KM,
                decimalPlaces = 2
            )
        )
        DatasetType.SALINITY -> listOf(
            DatasetVariable(
                id = "sss",
                displayName = "Salinity",
                zarrVariableName = "sss",
                rangeKey = "sss",
                hasPMTilesData = true,
                unit = DatasetUnit.PSU
            ),
            DatasetVariable(
                id = "salinity_gradient",
                displayName = "Break",
                zarrVariableName = "salinity_gradient",
                rangeKey = "salinity_gradient",
                hasPMTilesData = false,
                unit = DatasetUnit.PSU_PER_KM,
                decimalPlaces = 2
            )
        )
        DatasetType.SEA_SURFACE_HEIGHT -> listOf(
            DatasetVariable(
                id = "ssh",
                displayName = "Height",
                zarrVariableName = "sea_surface_height",
                rangeKey = "sea_surface_height",
                hasPMTilesData = true,
                unit = DatasetUnit.CENTIMETERS,
                decimalPlaces = 0
            ),
            DatasetVariable(
                id = "sla_gradient",
                displayName = "Break",
                zarrVariableName = "sla_gradient",
                rangeKey = "sla_gradient",
                hasPMTilesData = false,
                isVisible = false,
                unit = DatasetUnit.CM_PER_KM,
                decimalPlaces = 2
            ),
            DatasetVariable(
                id = "ugos",
                displayName = "U Velocity",
                zarrVariableName = "ugos",
                rangeKey = "ugos",
                hasPMTilesData = false,
                isVisible = false,
                unit = DatasetUnit.METERS_PER_SECOND,
                decimalPlaces = 2
            ),
            DatasetVariable(
                id = "vgos",
                displayName = "V Velocity",
                zarrVariableName = "vgos",
                rangeKey = "vgos",
                hasPMTilesData = false,
                isVisible = false,
                unit = DatasetUnit.METERS_PER_SECOND,
                decimalPlaces = 2
            ),
            DatasetVariable(
                id = "ugosa",
                displayName = "U Anomaly",
                zarrVariableName = "ugosa",
                rangeKey = "ugosa",
                hasPMTilesData = false,
                isVisible = false,
                unit = DatasetUnit.METERS_PER_SECOND,
                decimalPlaces = 2
            ),
            DatasetVariable(
                id = "vgosa",
                displayName = "V Anomaly",
                zarrVariableName = "vgosa",
                rangeKey = "vgosa",
                hasPMTilesData = false,
                isVisible = false,
                unit = DatasetUnit.METERS_PER_SECOND,
                decimalPlaces = 2
            ),
            DatasetVariable(
                id = "rotational_speed",
                displayName = "Rotation",
                zarrVariableName = "rotational_speed",
                rangeKey = "rotational_speed",
                hasPMTilesData = false,
                colorscale = Colorscale.MAGNITUDE,
                scaleMode = ScaleMode.LINEAR,
                unit = DatasetUnit.KNOTS,
                decimalPlaces = 2
            )
        )
        DatasetType.MLD -> listOf(
            DatasetVariable(
                id = "mld",
                displayName = "Depth",
                zarrVariableName = "mlotst",
                rangeKey = "mlotst",
                hasPMTilesData = true,
                unit = DatasetUnit.METERS,
                decimalPlaces = 0
            ),
            DatasetVariable(
                id = "mld_gradient",
                displayName = "Break",
                zarrVariableName = "mld_gradient",
                rangeKey = "mld_gradient",
                hasPMTilesData = false,
                isVisible = false,
                unit = DatasetUnit.METERS,
                decimalPlaces = 2
            )
        )
        DatasetType.CURRENTS -> listOf(
            DatasetVariable(
                id = "speed",
                displayName = "Speed",
                zarrVariableName = "speed",
                rangeKey = "speed",
                hasPMTilesData = true,
                unit = DatasetUnit.KNOTS,
                decimalPlaces = 2
            )
        )
        DatasetType.CHLOROPHYLL -> listOf(
            DatasetVariable(
                id = "chlor_a",
                displayName = "Chlorophyll",
                zarrVariableName = "chlor_a",
                rangeKey = "chlor_a",
                hasPMTilesData = true,
                unit = DatasetUnit.MG_PER_CUBIC_METER,
                decimalPlaces = 2
            ),
            DatasetVariable(
                id = "chlor_a_gradient",
                displayName = "Break",
                zarrVariableName = "chlor_a_gradient",
                rangeKey = "chlor_a_gradient",
                hasPMTilesData = false,
                isVisible = false, // Hidden until pipeline produces chlor_a_gradient
                unit = DatasetUnit.MG_PER_CUBIC_METER,
                decimalPlaces = 2
            )
        )
        DatasetType.WATER_CLARITY -> listOf(
            DatasetVariable(
                id = "kd_490",
                displayName = "Kd 490",
                zarrVariableName = "Kd_490",
                rangeKey = "Kd_490",
                hasPMTilesData = true,
                unit = DatasetUnit.INVERSE_METERS,
                decimalPlaces = 3
            )
        )
        DatasetType.WATER_TYPE -> listOf(
            DatasetVariable(
                id = "water_type",
                displayName = "Type",
                zarrVariableName = "label",
                rangeKey = "label",
                hasPMTilesData = true,
                unit = DatasetUnit.DIMENSIONLESS,
                decimalPlaces = 0
            )
        )
        DatasetType.FSLE -> listOf(
            DatasetVariable(
                id = "fsle",
                displayName = "FSLE",
                zarrVariableName = "fsle",
                rangeKey = "fsle",
                hasPMTilesData = true,
                unit = DatasetUnit.INVERSE_SECONDS,
                decimalPlaces = 2
            )
        )
        DatasetType.DISSOLVED_OXYGEN -> listOf(
            DatasetVariable(
                id = "o2",
                displayName = "O₂",
                zarrVariableName = "o2",
                rangeKey = "o2",
                hasPMTilesData = true,
                unit = DatasetUnit.MG_PER_LITER,
                decimalPlaces = 2
            )
        )
        DatasetType.PHYTOPLANKTON -> listOf(
            DatasetVariable(
                id = "phyc",
                displayName = "Phyto",
                zarrVariableName = "phyc",
                rangeKey = "phyc",
                hasPMTilesData = true,
                unit = DatasetUnit.MMOL_PER_CUBIC_METER,
                decimalPlaces = 2
            )
        )
    }

/** Variables shown in UI pickers. */
val DatasetType.displayVariables: List<DatasetVariable>
    get() = availableVariables.filter { it.isVisible }

/** Whether this dataset type supports variable switching (has 2+ user-facing variables) */
val DatasetType.hasMultipleVariables: Boolean
    get() = displayVariables.size > 1

/** Default (primary) variable for this dataset type */
val DatasetType.primaryVariable: DatasetVariable
    get() {
        require(displayVariables.isNotEmpty()) {
            "displayVariables is empty for $this — check that filter IDs match availableVariables"
        }
        return displayVariables.first()
    }
