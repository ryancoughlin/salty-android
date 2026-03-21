package com.example.saltyoffshore.data

import android.graphics.Color
import com.example.saltyoffshore.ui.theme.ColorScales

/**
 * Colorscale model - works with any dataset type.
 * Contains hex colors, TiTiler ID, and category for UI organization.
 */
data class Colorscale(
    val id: String,           // TiTiler colormap_name (e.g., "sst_high_contrast")
    val name: String,         // Display name (e.g., "SST")
    val hexColors: List<String>,
    val category: ColorscaleCategory
) {
    /**
     * Convert hex colors to Android Color ints for rendering
     */
    val colors: List<Int> by lazy {
        hexColors.map { parseHexColor(it) }
    }

    companion object {
        fun parseHexColor(hex: String): Int {
            val cleanHex = hex.removePrefix("#")
            return Color.parseColor("#$cleanHex")
        }

        // =============================================
        // TEMPERATURE SCALES
        // =============================================

        val SST = Colorscale(
            id = "sst_high_contrast",
            name = "SST",
            hexColors = ColorScales.sst,
            category = ColorscaleCategory.COLORFUL
        )

        val SALTY_VIBES = Colorscale(
            id = "salty_vibes",
            name = "Salty Vibes",
            hexColors = ColorScales.saltyVibes,
            category = ColorscaleCategory.COLORFUL
        )

        val THERMAL = Colorscale(
            id = "thermal",
            name = "Thermal",
            hexColors = ColorScales.thermal,
            category = ColorscaleCategory.COLORFUL
        )

        // =============================================
        // CHLOROPHYLL / PRODUCTIVITY SCALES
        // =============================================

        val CHLOROPHYLL = Colorscale(
            id = "chlorophyll",
            name = "Chlorophyll",
            hexColors = ColorScales.chlorophyll,
            category = ColorscaleCategory.COLORFUL
        )

        val GREENS = Colorscale(
            id = "greens",
            name = "Greens",
            hexColors = ColorScales.greens,
            category = ColorscaleCategory.SINGLE_COLOR
        )

        val BLOOM = Colorscale(
            id = "bloom",
            name = "Bloom",
            hexColors = ColorScales.bloom,
            category = ColorscaleCategory.COLORFUL
        )

        // =============================================
        // CURRENTS / SEA SURFACE HEIGHT SCALES
        // =============================================

        val CURRENTS = Colorscale(
            id = "currents",
            name = "Currents",
            hexColors = ColorScales.currents,
            category = ColorscaleCategory.COLORFUL
        )

        val RDBU = Colorscale(
            id = "bwr",  // TiTiler uses "bwr" for this colormap
            name = "RdBu",
            hexColors = ColorScales.rdbu,
            category = ColorscaleCategory.COLORFUL
        )

        val SPECTRAL = Colorscale(
            id = "spectral",
            name = "Spectral",
            hexColors = ColorScales.spectral,
            category = ColorscaleCategory.COLORFUL
        )

        // =============================================
        // MIXED LAYER DEPTH
        // =============================================

        val CASCADE = Colorscale(
            id = "cascade",
            name = "Cascade",
            hexColors = ColorScales.cascade,
            category = ColorscaleCategory.COLORFUL
        )

        // =============================================
        // SALINITY
        // =============================================

        val FLOW = Colorscale(
            id = "flow",
            name = "Flow",
            hexColors = ColorScales.flow,
            category = ColorscaleCategory.COLORFUL
        )

        // =============================================
        // DEPTH SCALES
        // =============================================

        val BLUES = Colorscale(
            id = "blues",
            name = "Blues",
            hexColors = ColorScales.blues,
            category = ColorscaleCategory.SINGLE_COLOR
        )

        // =============================================
        // UNIVERSAL
        // =============================================

        val VIRIDIS = Colorscale(
            id = "viridis",
            name = "Viridis",
            hexColors = ColorScales.viridis,
            category = ColorscaleCategory.COLORFUL
        )

        // =============================================
        // OVERLAY SCALES (Neutral)
        // =============================================

        val GREYS = Colorscale(
            id = "greys",
            name = "Greys",
            hexColors = ColorScales.greys,
            category = ColorscaleCategory.NEUTRAL
        )

        val BONE = Colorscale(
            id = "bone",
            name = "Bone",
            hexColors = ColorScales.bone,
            category = ColorscaleCategory.NEUTRAL
        )

        // =============================================
        // OVERLAY SCALES (Single Color)
        // =============================================

        val PURPLE = Colorscale(
            id = "purple",
            name = "Purple",
            hexColors = ColorScales.purpleOverlay,
            category = ColorscaleCategory.SINGLE_COLOR
        )

        val MAGENTA = Colorscale(
            id = "magenta",
            name = "Magenta",
            hexColors = ColorScales.magentaOverlay,
            category = ColorscaleCategory.SINGLE_COLOR
        )

        val CYAN = Colorscale(
            id = "cyan",
            name = "Cyan",
            hexColors = ColorScales.cyanOverlay,
            category = ColorscaleCategory.SINGLE_COLOR
        )

        val YELLOW = Colorscale(
            id = "yellow",
            name = "Yellow",
            hexColors = ColorScales.yellowOverlay,
            category = ColorscaleCategory.SINGLE_COLOR
        )

        val LIME = Colorscale(
            id = "lime",
            name = "Lime",
            hexColors = ColorScales.limeOverlay,
            category = ColorscaleCategory.SINGLE_COLOR
        )

        // =============================================
        // TITILER CUSTOM COLORMAPS
        // =============================================

        val BOUNDARY_FIRE = Colorscale(
            id = "boundary_fire",
            name = "Boundary Fire",
            hexColors = ColorScales.boundaryFire,
            category = ColorscaleCategory.COLORFUL
        )

        val MAGNITUDE = Colorscale(
            id = "magnitude",
            name = "Magnitude",
            hexColors = ColorScales.magnitude,
            category = ColorscaleCategory.COLORFUL
        )

        val ICE = Colorscale(
            id = "ice",
            name = "Ice",
            hexColors = ColorScales.ice,
            category = ColorscaleCategory.COLORFUL
        )

        // =============================================
        // WIND & WAVE SCALES
        // =============================================

        val WIND = Colorscale(
            id = "wind",
            name = "Wind",
            hexColors = ColorScales.wind,
            category = ColorscaleCategory.COLORFUL
        )

        val WAVE_HEIGHT = Colorscale(
            id = "wave_height",
            name = "Wave Height",
            hexColors = ColorScales.waveHeight,
            category = ColorscaleCategory.COLORFUL
        )

        val WAVE_PERIOD = Colorscale(
            id = "wave_period",
            name = "Wave Period",
            hexColors = ColorScales.wavePeriod,
            category = ColorscaleCategory.COLORFUL
        )

        // =============================================
        // WATER CLARITY
        // =============================================

        val WATER_CLARITY = Colorscale(
            id = "water_clarity",
            name = "Water Clarity",
            hexColors = ColorScales.waterClarity,
            category = ColorscaleCategory.COLORFUL
        )

        // =============================================
        // ALL COLORSCALES LIST
        // =============================================

        val ALL: List<Colorscale> = listOf(
            // Temperature
            SST, SALTY_VIBES, THERMAL,
            // Chlorophyll/Productivity
            CHLOROPHYLL, BLOOM, GREENS,
            // Currents & Sea Surface Height
            CURRENTS, SPECTRAL, RDBU,
            // Mixed Layer Depth
            CASCADE,
            // Salinity
            FLOW,
            // Depth
            BLUES,
            // Overlays (Neutral)
            GREYS, BONE,
            // Overlays (Single Color)
            PURPLE, MAGENTA, CYAN, YELLOW, LIME,
            // Wind & Wave
            WIND, WAVE_HEIGHT, WAVE_PERIOD,
            // Water Clarity
            WATER_CLARITY,
            // TiTiler Custom
            BOUNDARY_FIRE, MAGNITUDE, ICE,
            // Universal
            VIRIDIS
        )

        // =============================================
        // CATEGORY HELPERS
        // =============================================

        val singleColorScales: List<Colorscale>
            get() = ALL.filter { it.category == ColorscaleCategory.SINGLE_COLOR }

        val neutralScales: List<Colorscale>
            get() = ALL.filter { it.category == ColorscaleCategory.NEUTRAL }

        val colorfulScales: List<Colorscale>
            get() = ALL.filter { it.category == ColorscaleCategory.COLORFUL }

        /**
         * Find colorscale by TiTiler ID
         */
        fun fromId(id: String): Colorscale? = ALL.find { it.id == id }
    }
}
