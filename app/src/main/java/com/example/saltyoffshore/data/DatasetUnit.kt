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
    INVERSE_METERS("m⁻¹"),
    DIMENSIONLESS(""),
    INVERSE_SECONDS("s⁻¹"),
    MG_PER_LITER("mg/L"),
    CELSIUS_PER_KM("°C/km"),
    PSU_PER_KM("PSU/km"),
    CM_PER_KM("cm/km");

    companion object {
        fun fromString(value: String): DatasetUnit {
            return entries.find { it.symbol == value } ?: DIMENSIONLESS
        }
    }
}
