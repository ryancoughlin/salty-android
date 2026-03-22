package com.example.saltyoffshore.data.waypoint

/**
 * Options for controlling GPX file imports.
 * Ported from iOS GPXImportOptions.swift.
 */
data class GPXImportOptions(
    val symbolMapping: SymbolMappingStrategy = SymbolMappingStrategy.Autodetect,
    val duplicateHandling: DuplicateHandlingStrategy = DuplicateHandlingStrategy.SkipDuplicates,
    val includeEnvData: Boolean = true
) {
    sealed class SymbolMappingStrategy {
        /** Auto-detect symbols based on GPX content */
        data object Autodetect : SymbolMappingStrategy()

        /** Use specific mapping rules (configurable) */
        data object UseCustomMapping : SymbolMappingStrategy()

        /** Use a default symbol for all imported waypoints */
        data class UseDefaultSymbol(val symbol: WaypointSymbol) : SymbolMappingStrategy()

        /** Preserve original symbol information in notes field */
        data object PreserveInNotes : SymbolMappingStrategy()
    }

    enum class DuplicateHandlingStrategy {
        /** Skip waypoints with the same name and coordinates */
        SkipDuplicates,

        /** Replace waypoints with the same name and coordinates */
        ReplaceDuplicates,

        /** Always add imported waypoints, even if duplicates */
        AlwaysAdd
    }
}
