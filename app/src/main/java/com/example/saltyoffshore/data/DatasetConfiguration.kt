package com.example.saltyoffshore.data

/**
 * Configuration for rendering and querying dataset values.
 * Matches iOS DatasetConfiguration exactly.
 */
data class DatasetConfiguration(
    val valueKey: String,
    val unit: DatasetUnit,
    val decimalPlaces: Int,
    val labelPrefix: String = ""
) {
    companion object {
        fun forDatasetType(type: DatasetType): DatasetConfiguration {
            return when (type) {
                DatasetType.SST -> DatasetConfiguration(
                    valueKey = "temperature",
                    unit = DatasetUnit.FAHRENHEIT,
                    decimalPlaces = 1
                )
                DatasetType.CURRENTS -> DatasetConfiguration(
                    valueKey = "speed",
                    unit = DatasetUnit.KNOTS,
                    decimalPlaces = 2
                )
                DatasetType.CHLOROPHYLL -> DatasetConfiguration(
                    valueKey = "concentration",
                    unit = DatasetUnit.MG_PER_CUBIC_METER,
                    decimalPlaces = 2
                )
                DatasetType.SEA_SURFACE_HEIGHT -> DatasetConfiguration(
                    valueKey = "ssh",
                    unit = DatasetUnit.CENTIMETERS,
                    decimalPlaces = 0
                )
                DatasetType.PHYTOPLANKTON -> DatasetConfiguration(
                    valueKey = "phytoplankton",
                    unit = DatasetUnit.MG_PER_CUBIC_METER,
                    decimalPlaces = 2
                )
                DatasetType.WATER_CLARITY -> DatasetConfiguration(
                    valueKey = "Kd_490",
                    unit = DatasetUnit.INVERSE_METERS,
                    decimalPlaces = 3
                )
                DatasetType.SALINITY -> DatasetConfiguration(
                    valueKey = "salinity",
                    unit = DatasetUnit.PSU,
                    decimalPlaces = 1
                )
                DatasetType.WATER_TYPE -> DatasetConfiguration(
                    valueKey = "label",
                    unit = DatasetUnit.DIMENSIONLESS,
                    decimalPlaces = 0
                )
                DatasetType.MLD -> DatasetConfiguration(
                    valueKey = "depth",
                    unit = DatasetUnit.METERS,
                    decimalPlaces = 0
                )
                DatasetType.FSLE -> DatasetConfiguration(
                    valueKey = "fsle",
                    unit = DatasetUnit.PER_DAY,
                    decimalPlaces = 2
                )
                DatasetType.DISSOLVED_OXYGEN -> DatasetConfiguration(
                    valueKey = "dissolved_oxygen",
                    unit = DatasetUnit.MG_PER_LITER,
                    decimalPlaces = 2
                )
            }
        }
    }
}
