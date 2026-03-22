package com.example.saltyoffshore.data

import com.example.saltyoffshore.zarr.ColormapTextureFactory

/**
 * Bundles scale mode + colorscale + filter snapping + domain strategy for a dataset's rendering.
 * Single primitive that flows from DatasetType to all visual consumers:
 * - ZarrShaderHost (heatmap layer)
 * - MapboxParticleRenderer (particles)
 * - GradientScaleBar (UI indicator)
 * - FilterGradientBar (filter controls)
 *
 * Shaders use scaleMode.rawValue directly — no translation layer.
 */
data class RenderingConfig(
    val scaleMode: ScaleMode,
    val colorscale: Colorscale,
    /**
     * Filter drag snap increment. null = no snapping (for non-linear scales where
     * fixed increments fight scale compression). Display precision uses numberDecimalPlaces.
     */
    val snapIncrement: Double? = null,
    /**
     * How the color scale domain is determined. Either a fixed range
     * or percentile-clipped aggregate from API entry ranges.
     */
    val domainStrategy: DomainStrategy = DomainStrategy.Default,
    /**
     * How color stops are positioned in the 256-pixel colormap texture.
     * .Uniform = evenly spaced (default). .Log10 = positioned at log10(value),
     * matching TiTiler's create_log10_positioned_colormap.
     */
    val colormapDistribution: ColormapTextureFactory.StopDistribution = ColormapTextureFactory.StopDistribution.Uniform
)

/**
 * Unified rendering config: scaleMode + colorscale + snapIncrement.
 * Single source of truth — change one line here to update everywhere:
 * ZarrShaderHost, particles, GradientScaleBar, FilterGradientBar.
 */
val DatasetType.renderingConfig: RenderingConfig
    get() = when (this) {
        DatasetType.SST -> RenderingConfig(
            scaleMode = ScaleMode.LINEAR,
            colorscale = Colorscale.SST,
            snapIncrement = 0.1
        )

        DatasetType.CURRENTS -> RenderingConfig(
            // Sqrt: expands slow current detail, no snap (scale compression)
            scaleMode = ScaleMode.SQRT,
            colorscale = Colorscale.CURRENTS
        )

        DatasetType.CHLOROPHYLL -> {
            // Log10: fixed domain 0.01-8.0 mg/m³ matching TiTiler rescale=-2.0,0.903
            // Stop values match CHLOROPHYLL_LOG10_STOPS in server colors.py
            val stops = listOf(
                0.01f, 0.02f, 0.03f, 0.05f, 0.07f, 0.10f,
                0.12f, 0.15f, 0.18f, 0.22f, 0.28f, 0.35f, 0.42f, 0.50f,
                0.60f, 0.70f, 0.80f, 0.90f, 1.00f, 1.20f, 1.50f, 1.80f, 2.00f,
                3.00f, 4.00f, 5.00f, 6.00f, 7.00f, 8.00f
            )
            val domain = 0.01f..8.0f
            RenderingConfig(
                scaleMode = ScaleMode.LOGARITHMIC,
                colorscale = Colorscale.CHLOROPHYLL,
                domainStrategy = DomainStrategy.Fixed(domain),
                colormapDistribution = ColormapTextureFactory.StopDistribution.Log10(stops, domain)
            )
        }

        DatasetType.SEA_SURFACE_HEIGHT -> RenderingConfig(
            // Diverging: zero-centered, no snap (scale compression)
            scaleMode = ScaleMode.DIVERGING,
            colorscale = Colorscale.RDBU
        )

        DatasetType.PHYTOPLANKTON -> RenderingConfig(
            scaleMode = ScaleMode.LOGARITHMIC,
            colorscale = Colorscale.BLOOM,
            domainStrategy = DomainStrategy.Fixed(0.01f..8.0f)
        )

        DatasetType.WATER_CLARITY -> RenderingConfig(
            scaleMode = ScaleMode.LINEAR,
            colorscale = Colorscale.VIRIDIS,
            snapIncrement = 0.001
        )

        DatasetType.SALINITY -> RenderingConfig(
            scaleMode = ScaleMode.LINEAR,
            colorscale = Colorscale.FLOW,
            snapIncrement = 0.1
        )

        DatasetType.WATER_TYPE -> RenderingConfig(
            scaleMode = ScaleMode.LINEAR,
            colorscale = Colorscale.VIRIDIS,
            snapIncrement = 0.1
        )

        DatasetType.MLD -> RenderingConfig(
            scaleMode = ScaleMode.LINEAR,
            colorscale = Colorscale.CASCADE,
            snapIncrement = 1.0
        )

        DatasetType.FSLE -> RenderingConfig(
            scaleMode = ScaleMode.LINEAR,
            colorscale = Colorscale.SALTY_VIBES,
            snapIncrement = 0.01
        )

        DatasetType.DISSOLVED_OXYGEN -> RenderingConfig(
            scaleMode = ScaleMode.LINEAR,
            colorscale = Colorscale.BOUNDARY_FIRE,
            snapIncrement = 0.01
        )
    }

/**
 * Resolve rendering config for a specific variable.
 * Variable overrides (colorscale, scaleMode) take priority, everything else inherits.
 * iOS ref: DatasetType.renderingConfig(for:)
 */
fun DatasetType.renderingConfig(variable: DatasetVariable): RenderingConfig {
    val base = renderingConfig
    return RenderingConfig(
        scaleMode = variable.scaleMode ?: base.scaleMode,
        colorscale = variable.colorscale ?: base.colorscale,
        snapIncrement = base.snapIncrement,
        domainStrategy = base.domainStrategy,
        colormapDistribution = base.colormapDistribution
    )
}

/**
 * Scale mode derived from renderingConfig (for backwards compatibility).
 */
val DatasetType.scaleMode: ScaleMode
    get() = renderingConfig.scaleMode
