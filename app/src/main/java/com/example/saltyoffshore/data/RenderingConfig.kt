package com.example.saltyoffshore.data

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
    val domainStrategy: DomainStrategy = DomainStrategy.Default
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

        DatasetType.CHLOROPHYLL -> RenderingConfig(
            // Log10: fixed domain 0.01-8.0 mg/m³ matching TiTiler rescale=-2.0,0.903
            scaleMode = ScaleMode.LOGARITHMIC,
            colorscale = Colorscale.CHLOROPHYLL,
            domainStrategy = DomainStrategy.Fixed(0.01f..8.0f)
        )

        DatasetType.EDDYS -> RenderingConfig(
            // Diverging: zero-centered, no snap (scale compression)
            scaleMode = ScaleMode.DIVERGING,
            colorscale = Colorscale.RDBU
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
 * Scale mode derived from renderingConfig (for backwards compatibility).
 */
val DatasetType.scaleMode: ScaleMode
    get() = renderingConfig.scaleMode
