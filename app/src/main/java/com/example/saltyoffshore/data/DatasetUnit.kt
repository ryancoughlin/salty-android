package com.example.saltyoffshore.data

/**
 * Units for dataset values.
 * Matches iOS DatasetUnit exactly.
 */
enum class DatasetUnit(val symbol: String) {
    FAHRENHEIT("°F"),
    CELSIUS("°C"),
    KNOTS("kts"),
    METERS_PER_SECOND("m/s"),
    MG_PER_CUBIC_METER("mg/m³"),
    METERS("m"),
    CENTIMETERS("cm"),
    PER_DAY("day⁻¹"),
    PSU("PSU"),
    MMOL_PER_CUBIC_METER("mmol/m³"),
    PER_METER("m⁻¹"),
    UNITLESS("");

    companion object {
        fun fromString(value: String): DatasetUnit {
            return entries.find { it.symbol == value } ?: UNITLESS
        }
    }
}
