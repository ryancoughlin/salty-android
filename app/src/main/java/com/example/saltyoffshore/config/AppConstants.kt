package com.example.saltyoffshore.config

import com.mapbox.geojson.Point
import java.net.URLEncoder

/**
 * Application-wide constants following Android best practices.
 * 
 * Converted from iOS Swift AppConstants enum.
 * Uses object pattern (singleton) for constants class.
 */
object AppConstants {

    // MARK: - Feature Flags

    /**
     * Enable visual (COG) layers for overlays
     * - false: Contours only (STABLE - ship to App Store)
     * - true: Visual + Contours (NEW - internal testing only)
     */
    const val enableOverlayVisualLayers: Boolean = false

    // MARK: - Map Defaults

    /** Default zoom level for map */
    const val mapDefaultZoom: Double = 5.0

    /** Overview zoom level for map */
    const val mapOverviewZoom: Double = 3.0

    /** Initial world zoom level (hemisphere view for startup) */
    const val mapInitialWorldZoom: Double = 1.0

    /** Minimum zoom level */
    const val mapMinZoom: Double = 0.0

    /** Maximum zoom level */
    const val mapMaxZoom: Double = 12.0

    /** Default animation duration in milliseconds */
    const val mapDefaultAnimationDuration: Long = 1500L

    // MARK: - Map Themes

    /** Light theme map style URI */
    const val lightMapStyleURI: String = "mapbox://styles/snowcast/cm3rd1mik008801s97a8db8w6"

    /** Dark theme map style URI */
    const val darkMapStyleURI: String = "mapbox://styles/snowcast/cmfa1occi004701p83f5ghngx"

    /** Legacy - use lightMapStyleURI instead */
    const val mapStyleURI: String = lightMapStyleURI

    // MARK: - Server URLs

    /** Salty API base URL */
    const val apiBaseURL: String = "https://api.saltyoffshore.com"

    /** PMTiles server base URL */
    const val pmtilesBaseURL: String = "https://tiles.saltyoffshore.com"

    /** TiTiler server for COG processing and depth sampling */
    const val titilerBaseURL: String = "https://tiler.saltyoffshore.com"

    /** Static tiles server base URL */
    const val staticTilesURL: String = "https://tiles.saltyoffshore.com"

    /** CDN base URL for static assets */
    const val cdnBaseURL: String = "https://salty-data.nyc3.cdn.digitaloceanspaces.com"

    /** Data server base URL */
    const val dataBaseURL: String = "https://data.saltyoffshore.com"

    // MARK: - Global Layer Tile URLs

    /** Bathymetry contours PMTiles */
    const val bathymetryContoursURL: String = "$staticTilesURL/bathymetry_contours/{z}/{x}/{y}"

    /** Shipping lanes PMTiles */
    const val shippingLanesTileURL: String = "$staticTilesURL/shipping_lanes/{z}/{x}/{y}"

    /** Marine protected areas PMTiles */
    const val marineProtectedAreasTileURL: String = "$staticTilesURL/marine_protected_areas/{z}/{x}/{y}"

    /** GPS grid PMTiles */
    const val gpsGridURL: String = "$staticTilesURL/gps_grid/{z}/{x}/{y}"

    /** Shaded relief tile URL (TiTiler mosaic) */
    val shadedReliefTileURL: String by lazy {
        val mosaicUrl = "$cdnBaseURL/static/shaded_relief/shaded_relief_mosaic.json"
        val encoded = URLEncoder.encode(mosaicUrl, "UTF-8")
        "$titilerBaseURL/mosaicjson/tiles/WebMercatorQuad/{z}/{x}/{y}@2x.png?url=$encoded&bidx=1&bidx=2&bidx=3"
    }

    /** Shaded bathymetry tile URL (TiTiler mosaic with colormap) */
    val shadedBathymetryTileURL: String by lazy {
        val mosaicUrl = "$dataBaseURL/hillshaded_bathymetry/regional/raw_bathymetry_mosaic.json"
        val encoded = URLEncoder.encode(mosaicUrl, "UTF-8")
        "$titilerBaseURL/mosaicjson/tiles/WebMercatorQuad/{z}/{x}/{y}.png?url=$encoded&rescale=800,0&colormap_name=bathymetry&resampling=bilinear"
    }

    // MARK: - Map Configuration

    /**
     * Default map center coordinates.
     * Note: Mapbox Point uses longitude, latitude order (not latitude, longitude)
     */
    val mapDefaultCenter: Point = Point.fromLngLat(-74.0, 39.5)

    /** Map overview padding (equivalent to UIEdgeInsets) */
    val mapOverviewPadding: MapPadding = MapPadding(
        top = 6,
        start = 6,
        bottom = 6,
        end = 6
    )

    // MARK: - Supabase Configuration

    /** Supabase URL */
    const val supabaseUrl: String = "https://mvhhuhwoabqgrbqewqsk.supabase.co"

    /** Supabase service role key */
    const val supabaseKey: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im12aGh1aHdvYWJxZ3JicWV3cXNrIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTczMzAyMzU4NiwiZXhwIjoyMDQ4NTk5NTg2fQ.I8Ie0Zfk1KQgT54YVriSeQYvzd82DTTx12bbvYXRnNs"

    // MARK: - Helper Functions

    /**
     * Constructs a PMTiles tile URL with graceful fallback to legacy server.
     * 
     * Migration Strategy:
     * - New entries: Database provides full URL in `url` field (e.g., "https://tiles-atlantic.saltyoffshore.com/source_name")
     * - Old entries: Database has empty/null `url` field, falls back to legacy construction
     * 
     * @param pmtilesData The PMTiles data containing url and source
     * @return Full tile URL template with {z}/{x}/{y} placeholders
     */
    fun pmtilesTileURL(pmtilesData: PMTilesData): String {
        val urlFieldEmpty = pmtilesData.url.isEmpty()
        val isDataFileURL = pmtilesData.url.contains("data.")  // Direct file URL (old format)
        val isTileServerURL = pmtilesData.url.contains("tiles-") || 
                              pmtilesData.url.contains("tiles.")  // Martin server URL (new format)

        // Check URL format to determine construction strategy
        return if (!urlFieldEmpty && isTileServerURL) {
            // New format: URL points to Martin tile server - use directly
            "${pmtilesData.url}/{z}/{x}/{y}"
        } else {
            // Legacy fallback: URL is empty, points to data file, or unrecognized
            // Construct from base URL + source
            "$pmtilesBaseURL/${pmtilesData.source}/{z}/{x}/{y}"
        }
    }
}

