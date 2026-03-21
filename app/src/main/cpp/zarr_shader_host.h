#ifndef ZARR_SHADER_HOST_H
#define ZARR_SHADER_HOST_H

#include <GLES3/gl3.h>
#include <string>
#include <vector>
#include <memory>
#include <unordered_map>
#include <mutex>

/**
 * Fragment uniforms matching iOS FragmentUniforms (60 bytes).
 * Memory layout must match for consistent behavior.
 */
struct FragmentUniforms {
    float opacity;          // Layer opacity [0-1]
    float dataMin;          // Colormap min
    float dataMax;          // Colormap max
    float filterMin;        // Filter range min
    float filterMax;        // Filter range max
    int filterMode;         // 0=squash, 1=hideShow
    int scaleMode;          // 0=linear, 1=log10, 2=divergent, 3=sqrt
    float blendFactor;      // Crossfade: 0=prev, 1=current
};

/**
 * Cached frame data for instant timeline scrubbing.
 */
struct CachedFrame {
    GLuint texture;
    int width;
    int height;
    double swEasting;
    double swNorthing;
    double neEasting;
    double neNorthing;
    float dataMin;
    float dataMax;
};

/**
 * CPU-side frame data queued for GL texture creation.
 * Populated from any thread, consumed on GL render thread.
 */
struct PendingFrame {
    std::string entryId;
    std::vector<float> floats;
    int width;
    int height;
    double swEasting;
    double swNorthing;
    double neEasting;
    double neNorthing;
    float dataMin;
    float dataMax;
};

/**
 * Pending colormap data queued for GL texture creation.
 */
struct PendingColormap {
    std::vector<uint8_t> rgbaBytes;
    int size;
};

/**
 * ZarrShaderHost - OpenGL ES 3.0 implementation of CustomLayerHost.
 *
 * Thread safety:
 * - GL calls ONLY happen on Mapbox GL render thread (initialize, render, deinitialize)
 * - uploadFrame/showFrame queue data from any thread (IO dispatcher)
 * - render() drains pending queues on GL thread before drawing
 * - Mutex protects shared state between threads
 */
class ZarrShaderHost {
public:
    ZarrShaderHost();
    ~ZarrShaderHost();

    // CustomLayerHost lifecycle
    void initialize();
    void render(const double* projectionMatrix, double zoom);
    void deinitialize();

    // Frame management
    void uploadFrame(
        const std::string& entryId,
        const float* floats,
        int width, int height,
        double swEasting, double swNorthing,
        double neEasting, double neNorthing,
        float dataMin, float dataMax
    );
    bool showFrame(const std::string& entryId);
    void clearFrames();
    bool isLoaded(const std::string& entryId);

    // Colormap
    void setColormap(const uint8_t* rgbaBytes, int size);

    // Uniforms
    void setUniforms(
        float opacity,
        float filterMin, float filterMax,
        int filterMode, int scaleMode,
        float blendFactor
    );

    // Visibility
    void setVisible(bool visible);

private:
    // OpenGL resources
    GLuint program_ = 0;
    GLuint vao_ = 0;
    GLuint vbo_ = 0;
    GLuint colormapTexture_ = 0;

    // Current frame
    GLuint currentTexture_ = 0;
    GLuint prevTexture_ = 0;
    int currentWidth_ = 0;
    int currentHeight_ = 0;
    double currentSW_[2] = {0, 0};
    double currentNE_[2] = {0, 0};
    float currentDataMin_ = 0;
    float currentDataMax_ = 100;
    std::string currentFrameKey_;

    // Frame cache (GL textures, only accessed after processPendingFrames)
    std::unordered_map<std::string, CachedFrame> frames_;

    // Pending queues (populated from any thread, drained on GL thread)
    std::vector<PendingFrame> pendingFrames_;
    std::string pendingShowFrameKey_;
    std::unique_ptr<PendingColormap> pendingColormap_;

    // Uniforms
    FragmentUniforms uniforms_ = {1.0f, 0, 100, 0, 0, 0, 0, 1.0f};

    // State
    bool initialized_ = false;
    bool visible_ = true;
    bool pendingClear_ = false;

    // Thread safety
    mutable std::mutex mutex_;

    // GL thread only — called from render()
    void processPendingFrames();
    void processPendingColormap();

    // Shader helpers
    GLuint compileShader(GLenum type, const char* source);
    GLuint createProgram(const char* vertexSrc, const char* fragmentSrc);
    void setupGeometry();
    GLuint createDataTexture(const float* data, int width, int height);
};

#endif // ZARR_SHADER_HOST_H
