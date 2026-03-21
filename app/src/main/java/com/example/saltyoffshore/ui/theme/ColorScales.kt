package com.example.saltyoffshore.ui.theme

/**
 * Raw hex color arrays for each colorscale.
 * These match TiTiler server colormap names exactly.
 */
object ColorScales {

    /**
     * SST High Contrast - Standard high-contrast SST visualization (24 colors)
     * Temperature Range: Typically 65°F - 85°F
     */
    val sst = listOf(
        "#081d58", "#16306e", "#21449b", "#2c5fcf",
        "#3883f6", "#34d1db", "#0effc5", "#7ff000",
        "#ebf600", "#fec44f", "#fca23f", "#fb9137",
        "#fa802f", "#f96f27", "#f85e1f", "#f74d17",
        "#e6420e", "#d53e0d", "#c43a0c", "#b3360b",
        "#a2320a", "#912e09", "#802a08", "#5e2206"
    )

    /**
     * Salty Vibes - Extended range SST with purple cold-end (28 colors)
     * Temperature Range: Typically 60°F - 85°F
     */
    val saltyVibes = listOf(
        "#1a0033", "#35006b", "#5a00bb", "#081d58",
        "#16306e", "#21449b", "#2c5fcf", "#3883f6",
        "#34d1db", "#0effc5", "#7ff000", "#ebf600",
        "#fec44f", "#fca23f", "#fb9137", "#fa802f",
        "#f96f27", "#f85e1f", "#f74d17", "#e6420e",
        "#d53e0d", "#c43a0c", "#b3360b", "#a2320a",
        "#912e09", "#802a08", "#6f2607", "#5e2206"
    )

    /**
     * RdBu (Red-Blue) Diverging - Sea Surface Height / Eddies
     * Blue (low/negative) → White (zero) → Red (high/positive)
     */
    val rdbu = listOf(
        "#053061", "#2166ac", "#4393c3", "#92c5de", "#d1e5f0",
        "#f7f7f7",
        "#fddbc7", "#f4a582", "#d6604d", "#b2182b", "#67001f"
    )

    /**
     * Currents - Dark navy to red for ocean current speed
     */
    val currents = listOf(
        "#000814", "#001d3d", "#003566",
        "#0353a4", "#1e6091", "#2a6f97",
        "#2a9d8f", "#00b4d8", "#0096c7",
        "#00ced1", "#20b2aa", "#32cd32",
        "#9acd32", "#ffd700", "#ffa500",
        "#ff6347", "#ff4500", "#dc143c", "#b22222"
    )

    /**
     * Chlorophyll - 19 color stops with log10 scale from 0.01 to 8.0 mg/m³
     */
    val chlorophyll = listOf(
        "#E040E0", "#9966CC", "#6633CC", "#0D1F6D", "#1E3A8A",
        "#1E40AF", "#2196F3", "#3B82F6", "#00BCD4", "#00ACC1",
        "#00897B", "#26A69A", "#4CAF50", "#66BB6A", "#9CCC65",
        "#C0CA33", "#FDD835", "#FFB300", "#F57C00"
    )

    /**
     * Bloom - Phytoplankton bloom visualization (TiTiler colormap_name: "bloom")
     * Ultra-clear oligotrophic (purple/magenta) to intense blooms (red-brown)
     */
    val bloom = listOf(
        // Ultra-clear oligotrophic (deep purple/magenta - Gulf Stream type)
        "#4a0040", "#5c1a5c", "#6b2d6b",
        // Low productivity (deep blue)
        "#1a237e", "#283593", "#3949ab",
        // Clear to transitional (bright blue to cyan)
        "#1e88e5", "#039be5", "#00acc1", "#00bcd4",
        // Productive waters (cyan to teal to green)
        "#009688", "#00897b", "#26a69a", "#4caf50", "#66bb6a",
        // Dense productivity (yellow-green to yellow)
        "#8bc34a", "#9ccc65", "#c0ca33", "#cddc39", "#fdd835",
        // Bloom conditions (yellow to orange)
        "#ffb300", "#ffa000", "#ff8f00", "#ff6f00",
        // Intense blooms (deep orange to red-brown)
        "#e65100", "#d84315", "#bf360c", "#8d4004"
    )

    /**
     * Cascade - Mixed Layer Depth colorscale
     */
    val cascade = listOf(
        "#2d2d6b", "#1e4db8", "#2196f3", "#03a9f4", "#00bcd4",
        "#009688", "#4caf50", "#8bc34a", "#cddc39", "#ffc107",
        "#ff9800", "#f44336"
    )

    /**
     * Flow - Salinity colorscale
     */
    val flow = listOf(
        "#0a0d3a", "#0d1f6d", "#12328f", "#1746b1", "#1f7bbf",
        "#22a6c5", "#27c8b8", "#3fdf9b", "#87f27a", "#c9f560", "#f7f060"
    )

    /**
     * Viridis - Universal scientific colorscale
     */
    val viridis = listOf(
        "#440154", "#482878", "#3e4989", "#31688e", "#26828e",
        "#1f9e89", "#35b779", "#6ece58", "#b5de2b", "#fde725"
    )

    /**
     * Greys - Neutral greyscale for overlays
     */
    val greys = listOf(
        "#ffffff", "#f0f0f0", "#d9d9d9", "#bdbdbd", "#969696",
        "#737373", "#525252", "#252525", "#000000"
    )

    /**
     * Bone - Off-white/bone to dark grey for overlays
     */
    val bone = listOf(
        "#000000", "#141414", "#292929", "#3d3d3d", "#525252",
        "#666666", "#7a7a7a", "#b1b1b1", "#e8e8e6"
    )

    /**
     * Magnitude - FSLE/FTLE colorscale for front strength
     */
    val magnitude = listOf(
        "#0d1f6d", "#1a237e", "#3949ab", "#5c6bc0", "#7986cb",
        "#9fa8da", "#c5cae9", "#e8eaf6", "#b2dfdb", "#4db6ac",
        "#26a69a", "#81c784", "#c8e6c9", "#fff3e0", "#ffcc80",
        "#ff9800", "#f57c00", "#e65100", "#a30000"
    )

    /**
     * Boundary Fire - Dissolved Oxygen colorscale
     */
    val boundaryFire = listOf(
        "#8b008b", "#b22222", "#ff4500", "#ff8c00", "#ffd700",
        "#adff2f", "#00bfff", "#00008b", "#191970"
    )

    /**
     * Ice - Sequential colorscale (dark to light blue/white)
     */
    val ice = listOf(
        "#000000", "#0a0d1a", "#141b35", "#1e2850", "#28366b",
        "#324386", "#3c51a1", "#465ebc", "#506cd7", "#5a79f2",
        "#6497ff", "#8bb3ff", "#b2cfff", "#d9ebff", "#ffffff"
    )

    /**
     * Water Clarity (Kd_490)
     */
    val waterClarity = listOf(
        "#00204c", "#002b66", "#003780", "#00439a", "#004fb4",
        "#005bce", "#0067e8", "#0073ff", "#198eff", "#32a9ff",
        "#4bc4ff", "#64dfff", "#7dffff", "#7dffef", "#7dffdf",
        "#7dffcf", "#7dffbf", "#7dffaf", "#7dff9f", "#7dff8f",
        "#66ff66", "#4fff4f", "#38ff38", "#21ff21", "#0aff0a"
    )

    /**
     * Wind - Beaufort-style, 20 color stops, linear 0-30 m/s (0-58 kt)
     * Grey (calm) → blue → cyan → green → yellow → orange → red → purple (hurricane)
     */
    val wind = listOf(
        "#8395a7", "#6a89a7", "#5b9bd5", "#4a90d9", "#41b6c4",
        "#2c9f8f", "#37a132", "#6abf30", "#a2c523", "#d4c318",
        "#e8a910", "#ee8208", "#e85d0c", "#dc3912", "#cc2020",
        "#b01030", "#8b0a3a", "#9b30ff", "#7b20d0", "#5a108a"
    )

    /**
     * Wave Height - 10 color stops, linear 0-10 m
     * Cyan (calm) → teal → purple → magenta → red → dark red (phenomenal)
     */
    val waveHeight = listOf(
        "#80DEEA", "#26C6DA", "#00ACC1", "#0097A7", "#00838F",
        "#673AB7", "#9C27B0", "#E91E63", "#F44336", "#B71C1C"
    )

    /**
     * Wave Period - 10 color stops, linear 0-20 s
     * Cyan (short chop) → green (swell) → yellow → amber → orange (long period)
     */
    val wavePeriod = listOf(
        "#4DD0E1", "#26C6DA", "#00BCD4", "#00ACC1", "#009688",
        "#4CAF50", "#8BC34A", "#CDDC39", "#FFC107", "#FF9800"
    )

    // Single Color Overlay Scales
    val purpleOverlay = listOf("#f0e0ff", "#e0c0ff", "#c8a2c8", "#9d4edd", "#6a00cc")
    val magentaOverlay = listOf("#ffe0ff", "#ffc0ff", "#ff80ff", "#e600e6", "#990099")
    val cyanOverlay = listOf("#e0ffff", "#c0ffff", "#80ffff", "#00cccc", "#008080")
    val yellowOverlay = listOf("#ffffe0", "#ffffc0", "#ffff80", "#cccc00", "#999900")
    val limeOverlay = listOf("#f0ffe0", "#e0ffc0", "#c0ff80", "#99cc00", "#66aa00")

    /**
     * Spectral - Multi-hue diverging
     */
    val spectral = listOf(
        "#9e0142", "#d53e4f", "#f46d43", "#fdae61", "#fee08b",
        "#ffffbf", "#e6f598", "#abdda4", "#66c2a5", "#3288bd", "#5e4fa2"
    )

    /**
     * Blues - Single-hue sequential
     */
    val blues = listOf(
        "#f7fbff", "#deebf7", "#c6dbef", "#9ecae1", "#6baed6",
        "#4292c6", "#2171b5", "#08519c", "#08306b"
    )

    /**
     * Greens - Single-hue sequential
     */
    val greens = listOf(
        "#f7fcf5", "#e5f5e0", "#c7e9c0", "#a1d99b", "#74c476",
        "#41ab5d", "#238b45", "#006d2c", "#00441b"
    )

    /**
     * Thermal - Black to white through reds
     */
    val thermal = listOf(
        "#000000", "#330000", "#660000", "#990000", "#cc0000", "#ff0000",
        "#ff3300", "#ff6600", "#ff9900", "#ffcc00", "#ffff00", "#ffffff"
    )
}
