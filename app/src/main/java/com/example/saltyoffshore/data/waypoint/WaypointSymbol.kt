package com.example.saltyoffshore.data.waypoint

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// -- WaypointCategory --

enum class WaypointCategory(val displayName: String) {
    GARMIN("Garmin"),
    FISH("Fish"),
    STRUCTURE("Structure"),
    NAVIGATION("Navigation"),
    ENVIRONMENT("Environment"),
    OTHER("Other")
}

// -- WaypointSymbol --

@Serializable(with = WaypointSymbolSerializer::class)
enum class WaypointSymbol(val rawValue: String) {
    // Garmin Compatible Symbols
    RED_CIRCLE("Red Circle"),
    YELLOW_CIRCLE("Yellow Circle"),
    BLUE_CIRCLE("Blue Circle"),
    GREEN_CIRCLE("Green Circle"),
    RED_FLAG("Red Flag"),
    YELLOW_FLAG("Yellow Flag"),
    BLUE_FLAG("Blue Flag"),
    GREEN_FLAG("Green Flag"),
    RED_SQUARE("Red Square"),
    YELLOW_SQUARE("Yellow Square"),
    BLUE_SQUARE("Blue Square"),
    GREEN_SQUARE("Green Square"),
    RED_TRIANGLE("Red Triangle"),
    YELLOW_TRIANGLE("Yellow Triangle"),
    BLUE_TRIANGLE("Blue Triangle"),
    GREEN_TRIANGLE("Green Triangle"),
    DOT("Dot"),
    FISHING_AREA_1("Fishing Area 1"),
    FISHING_AREA_2("Fishing Area 2"),
    FISHING_AREA_3("Fishing Area 3"),
    FISHING_AREA_4("Fishing Area 4"),
    FISHING_AREA_5("Fishing Area 5"),
    FISHING_AREA_6("Fishing Area 6"),
    FISHING_AREA_7("Fishing Area 7"),
    FISHING_AREA_8("Fishing Area 8"),
    FISHING_AREA_9("Fishing Area 9"),

    // Legacy Symbols (deprecated but maintained for compatibility)
    BLUEFIN_TUNA("Bluefin Tuna"),
    YELLOWFIN_TUNA("Yellowfin Tuna"),
    BLACKFIN_TUNA("Blackfin Tuna"),
    BIG_EYE("Big Eye"),
    ALBACORE("Albacore"),
    BILLFISH("Billfish"),
    SAILFISH("Sailfish"),
    BLACK_MARLIN("Black Marlin"),
    BLUE_MARLIN("Blue Marlin"),
    STRIPED_MARLIN("Striped Marlin"),
    GROUPER("Grouper"),
    MAHI("Mahi"),
    WAHOO("Wahoo"),
    SEAMOUNT("Seamount"),
    SAND("Sand"),
    CORAL_REEF("Coral Reef"),
    FAD("FAD"),
    DROP_OFF("Drop-Off"),
    SHIPWRECK("Shipwreck"),
    OIL_PLATFORM("Oil Platform"),
    ANCHOR("Anchor"),
    BIRDS("Birds"),
    CHLOROPHYLL("Chlorophyll"),
    FLAG("Flag"),
    CAUTION("Caution"),
    DIVE("Dive");

    val imageName: String
        get() = when (this) {
            RED_CIRCLE -> "RedCircle"
            YELLOW_CIRCLE -> "YellowCircle"
            BLUE_CIRCLE -> "BlueCircle"
            GREEN_CIRCLE -> "GreenCircle"
            RED_FLAG -> "RedFlag"
            YELLOW_FLAG -> "YellowFlag"
            BLUE_FLAG -> "BlueFlag"
            GREEN_FLAG -> "GreenFlag"
            RED_SQUARE -> "RedSquare"
            YELLOW_SQUARE -> "YellowSquare"
            BLUE_SQUARE -> "BlueSquare"
            GREEN_SQUARE -> "GreenSquare"
            RED_TRIANGLE -> "RedTriangle"
            YELLOW_TRIANGLE -> "YellowTriangle"
            BLUE_TRIANGLE -> "BlueTriangle"
            GREEN_TRIANGLE -> "GreenTriangle"
            DOT -> "Dot"
            FISHING_AREA_1 -> "Fishing Area 1"
            FISHING_AREA_2 -> "Fishing Area 2"
            FISHING_AREA_3 -> "Fishing Area 3"
            FISHING_AREA_4 -> "Fishing Area 4"
            FISHING_AREA_5 -> "Fishing Area 5"
            FISHING_AREA_6 -> "Fishing Area 6"
            FISHING_AREA_7 -> "Fishing Area 7"
            FISHING_AREA_8 -> "Fishing Area 8"
            FISHING_AREA_9 -> "Fishing Area 9"
            BLUEFIN_TUNA -> "Marker-BluefinTuna"
            YELLOWFIN_TUNA -> "Marker-YellowfinTuna"
            BLACKFIN_TUNA -> "Marker-BlackfinTuna"
            BIG_EYE -> "Marker-BigEye"
            ALBACORE -> "Marker-Albacore"
            BILLFISH -> "Marker-Billfish"
            SAILFISH -> "Marker-Sailfish"
            BLACK_MARLIN -> "Marker-BlackMarlin"
            BLUE_MARLIN -> "Marker-BlueMarlin"
            STRIPED_MARLIN -> "Marker-StripedMarlin"
            GROUPER -> "Marker-Grouper"
            MAHI -> "Marker-Mahi"
            WAHOO -> "Marker-Wahoo"
            SEAMOUNT -> "Marker-Seamount"
            SHIPWRECK -> "Marker-Shipwreck"
            FAD -> "Marker-FAD"
            DROP_OFF -> "Marker-Dropoff"
            CORAL_REEF -> "Marker-Coral"
            OIL_PLATFORM -> "Marker-OilPlatform"
            ANCHOR -> "Marker-Anchor"
            BIRDS -> "Marker-Birds"
            CHLOROPHYLL -> "Marker-Chlorophyll"
            FLAG -> "Marker-Flag"
            SAND -> "Marker-Sand"
            CAUTION -> "Marker-Caution"
            DIVE -> "Marker-DiveFlag"
        }

    val category: WaypointCategory
        get() = when (this) {
            RED_CIRCLE, YELLOW_CIRCLE, BLUE_CIRCLE, GREEN_CIRCLE,
            RED_FLAG, YELLOW_FLAG, BLUE_FLAG, GREEN_FLAG,
            RED_SQUARE, YELLOW_SQUARE, BLUE_SQUARE, GREEN_SQUARE,
            RED_TRIANGLE, YELLOW_TRIANGLE, BLUE_TRIANGLE, GREEN_TRIANGLE,
            DOT,
            FISHING_AREA_1, FISHING_AREA_2, FISHING_AREA_3, FISHING_AREA_4, FISHING_AREA_5,
            FISHING_AREA_6, FISHING_AREA_7, FISHING_AREA_8, FISHING_AREA_9 -> WaypointCategory.GARMIN

            BLUEFIN_TUNA, YELLOWFIN_TUNA, BLACKFIN_TUNA, BIG_EYE, ALBACORE,
            BILLFISH, SAILFISH, BLACK_MARLIN, BLUE_MARLIN, STRIPED_MARLIN,
            GROUPER, MAHI, WAHOO -> WaypointCategory.FISH

            SEAMOUNT, SHIPWRECK, CORAL_REEF, FAD, DROP_OFF, SAND, OIL_PLATFORM -> WaypointCategory.STRUCTURE
            ANCHOR -> WaypointCategory.NAVIGATION
            BIRDS, CHLOROPHYLL -> WaypointCategory.ENVIRONMENT
            FLAG, CAUTION, DIVE -> WaypointCategory.OTHER
        }

    val shortName: String
        get() = when (this) {
            YELLOWFIN_TUNA -> "Yellowfin"
            BLUEFIN_TUNA -> "Bluefin"
            BLACKFIN_TUNA -> "Blackfin"
            BIG_EYE -> "Big Eye"
            ALBACORE -> "Albacore"
            BILLFISH -> "Billfish"
            SAILFISH -> "Sailfish"
            BLACK_MARLIN -> "Black Marlin"
            BLUE_MARLIN -> "Blue Marlin"
            STRIPED_MARLIN -> "Striped Marlin"
            GROUPER -> "Grouper"
            MAHI -> "Mahi"
            WAHOO -> "Wahoo"
            else -> rawValue
        }

    companion object {
        private val rawValueMap = entries.associateBy { it.rawValue }

        fun fromRawValue(value: String): WaypointSymbol {
            // Migration: deprecated symbols -> DOT
            val migrated = when (value) {
                "Waypoint", "Buoy" -> "Dot"
                else -> value
            }
            return rawValueMap[migrated] ?: DOT
        }

        val fishSpecies: List<WaypointSymbol> =
            entries.filter { it.category == WaypointCategory.FISH }

        val fishSpeciesOrdered: List<WaypointSymbol> = listOf(
            BLUE_MARLIN, BLACK_MARLIN, STRIPED_MARLIN, SAILFISH,
            YELLOWFIN_TUNA, BLUEFIN_TUNA, BIG_EYE, BLACKFIN_TUNA, ALBACORE,
            BILLFISH, MAHI, WAHOO, GROUPER
        )

        fun sortedByCategory(
            symbols: List<WaypointSymbol>,
            categoryOrder: List<WaypointCategory>
        ): List<WaypointSymbol> {
            return symbols.sortedWith(compareBy<WaypointSymbol> {
                val index = categoryOrder.indexOf(it.category)
                if (index == -1) categoryOrder.size else index
            }.thenBy { it.rawValue })
        }
    }
}

// -- Custom Serializer (handles migration from deprecated symbols) --

object WaypointSymbolSerializer : KSerializer<WaypointSymbol> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("WaypointSymbol", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): WaypointSymbol {
        val rawValue = decoder.decodeString()
        return WaypointSymbol.fromRawValue(rawValue)
    }

    override fun serialize(encoder: Encoder, value: WaypointSymbol) {
        encoder.encodeString(value.rawValue)
    }
}
