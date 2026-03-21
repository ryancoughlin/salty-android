#include "zarr_shader_host.h"
#include <android/log.h>
#include <cmath>
#include <cstring>
#include <vector>

#define LOG_TAG "ZarrShaderHost"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// MARK: - GLSL Shaders

static const char* VERTEX_SHADER = R"glsl(#version 300 es
precision highp float;

layout(location = 0) in vec2 a_position;  // World pixel coordinates
layout(location = 1) in vec2 a_texCoord;  // UV coordinates

uniform mat4 u_projectionMatrix;

out vec2 v_texCoord;

void main() {
    gl_Position = u_projectionMatrix * vec4(a_position, 0.0, 1.0);
    v_texCoord = a_texCoord;
}
)glsl";

static const char* FRAGMENT_SHADER = R"glsl(#version 300 es
precision highp float;
precision highp sampler2D;

in vec2 v_texCoord;
out vec4 fragColor;

uniform sampler2D u_dataTexture;
uniform sampler2D u_colormapTexture;
uniform sampler2D u_prevDataTexture;

uniform float u_opacity;
uniform float u_dataMin;
uniform float u_dataMax;
uniform float u_filterMin;
uniform float u_filterMax;
uniform int u_filterMode;
uniform int u_scaleMode;
uniform float u_blendFactor;
uniform vec2 u_textureSize;

// Check if value is valid (not NaN, not fill value)
bool isValidValue(float v) {
    return !isnan(v) && v < 1.0e30;
}

// Scale normalization functions (matching iOS ScaleNormalization.h)

float normalizeLinear(float v, float lo, float hi) {
    float range = hi - lo;
    if (range <= 0.0) return 0.5;
    return clamp((v - lo) / range, 0.0, 1.0);
}

float normalizeLog10(float v, float lo, float hi) {
    float clamped = max(v, 1e-6);
    float logVal = log(clamped) / log(10.0);
    float logMin = log(max(lo, 1e-6)) / log(10.0);
    float logMax = log(max(hi, 1e-6)) / log(10.0);
    float logRange = logMax - logMin;
    if (logRange <= 0.0) return 0.5;
    return clamp((logVal - logMin) / logRange, 0.0, 1.0);
}

float normalizeSqrt(float v, float lo, float hi) {
    float range = hi - lo;
    if (range <= 0.0) return 0.5;
    float linear = clamp((v - lo) / range, 0.0, 1.0);
    return sqrt(linear);
}

float normalizeDivergent(float v, float lo, float hi) {
    float center = 0.0;  // Zero-centered
    if (v <= center) {
        float range = center - lo;
        if (range <= 0.0) return 0.5;
        float t = (v - lo) / range;
        return clamp(t * 0.5, 0.0, 0.5);
    } else {
        float range = hi - center;
        if (range <= 0.0) return 0.5;
        float t = (v - center) / range;
        return clamp(0.5 + t * 0.5, 0.5, 1.0);
    }
}

float normalizeValue(float v, float lo, float hi, int scaleMode) {
    if (scaleMode == 1) return normalizeLog10(v, lo, hi);
    if (scaleMode == 2) return normalizeDivergent(v, lo, hi);
    if (scaleMode == 3) return normalizeSqrt(v, lo, hi);
    return normalizeLinear(v, lo, hi);
}

void main() {
    // Sample nearest pixel center for land masking
    vec2 pixelCoord = floor(v_texCoord * u_textureSize) + 0.5;
    vec2 nearestUV = pixelCoord / u_textureSize;
    float centerValue = texture(u_dataTexture, nearestUV).r;

    if (!isValidValue(centerValue)) {
        discard;
    }

    // Sample current frame (bilinear interpolation)
    float value = texture(u_dataTexture, v_texCoord).r;
    if (!isValidValue(value)) {
        value = centerValue;
    }

    // Blend with previous frame if crossfading
    if (u_blendFactor < 0.999) {
        float prevValue = texture(u_prevDataTexture, v_texCoord).r;
        if (isValidValue(prevValue)) {
            value = mix(prevValue, value, u_blendFactor);
        }
    }

    // Determine range (filter or data)
    bool filterActive = u_filterMin < u_filterMax;
    float rangeMin = filterActive ? u_filterMin : u_dataMin;
    float rangeMax = filterActive ? u_filterMax : u_dataMax;

    // Scale normalization
    float t = normalizeValue(value, rangeMin, rangeMax, u_scaleMode);

    // Filter alpha (hideShow mode)
    float filterAlpha = 1.0;
    if (u_filterMode == 1 && filterActive) {
        float filterRange = u_filterMax - u_filterMin;
        float filterEdge = 0.02 * filterRange;
        float minFade = smoothstep(u_filterMin - filterEdge, u_filterMin + filterEdge, value);
        float maxFade = smoothstep(u_filterMax + filterEdge, u_filterMax - filterEdge, value);
        filterAlpha = minFade * maxFade;
    }

    // Sample colormap
    vec4 color = texture(u_colormapTexture, vec2(t, 0.5));
    color.a = filterAlpha * u_opacity;

    fragColor = color;
}
)glsl";

// MARK: - Constructor/Destructor

ZarrShaderHost::ZarrShaderHost() {
    LOGI("ZarrShaderHost created");
}

ZarrShaderHost::~ZarrShaderHost() {
    deinitialize();
    LOGI("ZarrShaderHost destroyed");
}

// MARK: - CustomLayerHost Lifecycle

void ZarrShaderHost::initialize() {
    std::lock_guard<std::mutex> lock(mutex_);

    if (initialized_) return;

    LOGI("Initializing OpenGL ES 3.0 pipeline");

    // Create shader program
    program_ = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
    if (program_ == 0) {
        LOGE("Failed to create shader program");
        return;
    }

    // Setup geometry
    setupGeometry();

    // Create default colormap (grayscale)
    std::vector<uint8_t> grayscale(256 * 4);
    for (int i = 0; i < 256; i++) {
        grayscale[i * 4 + 0] = static_cast<uint8_t>(i);
        grayscale[i * 4 + 1] = static_cast<uint8_t>(i);
        grayscale[i * 4 + 2] = static_cast<uint8_t>(i);
        grayscale[i * 4 + 3] = 255;
    }
    setColormap(grayscale.data(), 256 * 4);

    initialized_ = true;
    LOGI("OpenGL ES 3.0 pipeline initialized");
}

void ZarrShaderHost::render(const double* projectionMatrix, double zoom) {
    std::lock_guard<std::mutex> lock(mutex_);

    if (!initialized_ || !visible_ || currentTexture_ == 0 || colormapTexture_ == 0) {
        return;
    }

    // Use our shader program
    glUseProgram(program_);

    // Convert projection matrix to float
    float proj[16];
    for (int i = 0; i < 16; i++) {
        proj[i] = static_cast<float>(projectionMatrix[i]);
    }

    // MARK: - Compute vertex positions (ProjectedMeters → world pixels)
    // Direct conversion from EPSG:3857 meters to normalized Mercator [0,1] to world pixels

    const double earthRadius = 20037508.34;
    const double earthDiameter = 40075016.68;

    // ProjectedMeters (EPSG:3857) → normalized Mercator [0,1]
    double swNormX = (currentSW_[0] + earthRadius) / earthDiameter;
    double swNormY = (earthRadius - currentSW_[1]) / earthDiameter;
    double neNormX = (currentNE_[0] + earthRadius) / earthDiameter;
    double neNormY = (earthRadius - currentNE_[1]) / earthDiameter;

    double worldSize = 512.0 * std::pow(2.0, zoom);

    float swX = static_cast<float>(swNormX * worldSize);
    float swY = static_cast<float>(swNormY * worldSize);
    float neX = static_cast<float>(neNormX * worldSize);
    float neY = static_cast<float>(neNormY * worldSize);

    // Build vertex data (position + UV)
    // Two triangles: SW-SE-NE, SW-NE-NW
    float vertices[] = {
        // Triangle 1: SW, SE, NE
        swX, swY,  0.0f, 1.0f,  // SW: UV (0, 1)
        neX, swY,  1.0f, 1.0f,  // SE: UV (1, 1)
        neX, neY,  1.0f, 0.0f,  // NE: UV (1, 0)
        // Triangle 2: SW, NE, NW
        swX, swY,  0.0f, 1.0f,  // SW: UV (0, 1)
        neX, neY,  1.0f, 0.0f,  // NE: UV (1, 0)
        swX, neY,  0.0f, 0.0f,  // NW: UV (0, 0)
    };

    // Upload vertex data
    glBindBuffer(GL_ARRAY_BUFFER, vbo_);
    glBufferSubData(GL_ARRAY_BUFFER, 0, sizeof(vertices), vertices);

    // Set uniforms
    glUniformMatrix4fv(glGetUniformLocation(program_, "u_projectionMatrix"), 1, GL_FALSE, proj);
    glUniform1f(glGetUniformLocation(program_, "u_opacity"), uniforms_.opacity);
    glUniform1f(glGetUniformLocation(program_, "u_dataMin"), currentDataMin_);
    glUniform1f(glGetUniformLocation(program_, "u_dataMax"), currentDataMax_);
    glUniform1f(glGetUniformLocation(program_, "u_filterMin"), uniforms_.filterMin);
    glUniform1f(glGetUniformLocation(program_, "u_filterMax"), uniforms_.filterMax);
    glUniform1i(glGetUniformLocation(program_, "u_filterMode"), uniforms_.filterMode);
    glUniform1i(glGetUniformLocation(program_, "u_scaleMode"), uniforms_.scaleMode);
    glUniform1f(glGetUniformLocation(program_, "u_blendFactor"), uniforms_.blendFactor);

    // Texture size for nearest-neighbor sampling (tracked, not queried - ES 3.0 lacks glGetTexLevelParameteriv)
    glUniform2f(glGetUniformLocation(program_, "u_textureSize"),
                static_cast<float>(currentWidth_), static_cast<float>(currentHeight_));

    // Bind textures
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, currentTexture_);
    glUniform1i(glGetUniformLocation(program_, "u_dataTexture"), 0);

    glActiveTexture(GL_TEXTURE1);
    glBindTexture(GL_TEXTURE_2D, colormapTexture_);
    glUniform1i(glGetUniformLocation(program_, "u_colormapTexture"), 1);

    glActiveTexture(GL_TEXTURE2);
    GLuint prevTex = (prevTexture_ != 0) ? prevTexture_ : currentTexture_;
    glBindTexture(GL_TEXTURE_2D, prevTex);
    glUniform1i(glGetUniformLocation(program_, "u_prevDataTexture"), 2);

    // Enable blending
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    // Draw
    glBindVertexArray(vao_);
    glDrawArrays(GL_TRIANGLES, 0, 6);
    glBindVertexArray(0);

    // Restore state
    glDisable(GL_BLEND);
    glUseProgram(0);
}

void ZarrShaderHost::deinitialize() {
    std::lock_guard<std::mutex> lock(mutex_);

    if (!initialized_) return;

    // Delete cached frames
    for (auto& pair : frames_) {
        if (pair.second.texture != 0) {
            glDeleteTextures(1, &pair.second.texture);
        }
    }
    frames_.clear();

    // Delete current textures
    if (currentTexture_ != 0) {
        glDeleteTextures(1, &currentTexture_);
        currentTexture_ = 0;
    }
    if (prevTexture_ != 0) {
        glDeleteTextures(1, &prevTexture_);
        prevTexture_ = 0;
    }
    if (colormapTexture_ != 0) {
        glDeleteTextures(1, &colormapTexture_);
        colormapTexture_ = 0;
    }

    // Delete geometry
    if (vao_ != 0) {
        glDeleteVertexArrays(1, &vao_);
        vao_ = 0;
    }
    if (vbo_ != 0) {
        glDeleteBuffers(1, &vbo_);
        vbo_ = 0;
    }

    // Delete program
    if (program_ != 0) {
        glDeleteProgram(program_);
        program_ = 0;
    }

    initialized_ = false;
    LOGI("OpenGL ES 3.0 pipeline deinitialized");
}

// MARK: - Frame Management

void ZarrShaderHost::uploadFrame(
    const std::string& entryId,
    const float* floats,
    int width, int height,
    double swEasting, double swNorthing,
    double neEasting, double neNorthing,
    float dataMin, float dataMax
) {
    std::lock_guard<std::mutex> lock(mutex_);

    // Skip if already cached
    if (frames_.find(entryId) != frames_.end()) {
        return;
    }

    GLuint texture = createDataTexture(floats, width, height);
    if (texture == 0) {
        LOGE("Failed to create texture for frame: %s", entryId.c_str());
        return;
    }

    CachedFrame frame;
    frame.texture = texture;
    frame.width = width;
    frame.height = height;
    frame.swEasting = swEasting;
    frame.swNorthing = swNorthing;
    frame.neEasting = neEasting;
    frame.neNorthing = neNorthing;
    frame.dataMin = dataMin;
    frame.dataMax = dataMax;

    frames_[entryId] = frame;
    LOGI("Uploaded frame: %s (%dx%d)", entryId.c_str(), width, height);
}

bool ZarrShaderHost::showFrame(const std::string& entryId) {
    std::lock_guard<std::mutex> lock(mutex_);

    if (entryId == currentFrameKey_) {
        return true;
    }

    auto it = frames_.find(entryId);
    if (it == frames_.end()) {
        LOGE("Frame not found: %s", entryId.c_str());
        return false;
    }

    const CachedFrame& frame = it->second;

    // Preserve current for crossfade
    prevTexture_ = currentTexture_;

    currentTexture_ = frame.texture;
    currentWidth_ = frame.width;
    currentHeight_ = frame.height;
    currentSW_[0] = frame.swEasting;
    currentSW_[1] = frame.swNorthing;
    currentNE_[0] = frame.neEasting;
    currentNE_[1] = frame.neNorthing;
    currentDataMin_ = frame.dataMin;
    currentDataMax_ = frame.dataMax;
    currentFrameKey_ = entryId;

    return true;
}

void ZarrShaderHost::clearFrames() {
    std::lock_guard<std::mutex> lock(mutex_);

    for (auto& pair : frames_) {
        if (pair.second.texture != 0) {
            glDeleteTextures(1, &pair.second.texture);
        }
    }
    frames_.clear();
    currentTexture_ = 0;
    prevTexture_ = 0;
    currentFrameKey_.clear();

    LOGI("Cleared all frames");
}

bool ZarrShaderHost::isLoaded(const std::string& entryId) {
    std::lock_guard<std::mutex> lock(mutex_);
    return frames_.find(entryId) != frames_.end();
}

// MARK: - Colormap

void ZarrShaderHost::setColormap(const uint8_t* rgbaBytes, int size) {
    std::lock_guard<std::mutex> lock(mutex_);

    int width = size / 4;  // RGBA = 4 bytes per pixel

    if (colormapTexture_ != 0) {
        glDeleteTextures(1, &colormapTexture_);
    }

    glGenTextures(1, &colormapTexture_);
    glBindTexture(GL_TEXTURE_2D, colormapTexture_);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, rgbaBytes);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

    LOGI("Colormap set: %d pixels", width);
}

// MARK: - Uniforms

void ZarrShaderHost::setUniforms(
    float opacity,
    float filterMin, float filterMax,
    int filterMode, int scaleMode,
    float blendFactor
) {
    std::lock_guard<std::mutex> lock(mutex_);

    uniforms_.opacity = opacity;
    uniforms_.filterMin = filterMin;
    uniforms_.filterMax = filterMax;
    uniforms_.filterMode = filterMode;
    uniforms_.scaleMode = scaleMode;
    uniforms_.blendFactor = blendFactor;
}

void ZarrShaderHost::setVisible(bool visible) {
    std::lock_guard<std::mutex> lock(mutex_);
    visible_ = visible;
}

// MARK: - Shader Helpers

GLuint ZarrShaderHost::compileShader(GLenum type, const char* source) {
    GLuint shader = glCreateShader(type);
    glShaderSource(shader, 1, &source, nullptr);
    glCompileShader(shader);

    GLint success;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &success);
    if (!success) {
        char infoLog[512];
        glGetShaderInfoLog(shader, 512, nullptr, infoLog);
        LOGE("Shader compilation failed: %s", infoLog);
        glDeleteShader(shader);
        return 0;
    }

    return shader;
}

GLuint ZarrShaderHost::createProgram(const char* vertexSrc, const char* fragmentSrc) {
    GLuint vertexShader = compileShader(GL_VERTEX_SHADER, vertexSrc);
    if (vertexShader == 0) return 0;

    GLuint fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentSrc);
    if (fragmentShader == 0) {
        glDeleteShader(vertexShader);
        return 0;
    }

    GLuint program = glCreateProgram();
    glAttachShader(program, vertexShader);
    glAttachShader(program, fragmentShader);
    glLinkProgram(program);

    GLint success;
    glGetProgramiv(program, GL_LINK_STATUS, &success);
    if (!success) {
        char infoLog[512];
        glGetProgramInfoLog(program, 512, nullptr, infoLog);
        LOGE("Program linking failed: %s", infoLog);
        glDeleteProgram(program);
        program = 0;
    }

    glDeleteShader(vertexShader);
    glDeleteShader(fragmentShader);

    return program;
}

void ZarrShaderHost::setupGeometry() {
    glGenVertexArrays(1, &vao_);
    glGenBuffers(1, &vbo_);

    glBindVertexArray(vao_);
    glBindBuffer(GL_ARRAY_BUFFER, vbo_);

    // Allocate buffer for 6 vertices (2 triangles), 4 floats each (position + UV)
    glBufferData(GL_ARRAY_BUFFER, 6 * 4 * sizeof(float), nullptr, GL_DYNAMIC_DRAW);

    // Position attribute (location = 0)
    glVertexAttribPointer(0, 2, GL_FLOAT, GL_FALSE, 4 * sizeof(float), (void*)0);
    glEnableVertexAttribArray(0);

    // UV attribute (location = 1)
    glVertexAttribPointer(1, 2, GL_FLOAT, GL_FALSE, 4 * sizeof(float), (void*)(2 * sizeof(float)));
    glEnableVertexAttribArray(1);

    glBindVertexArray(0);
}

GLuint ZarrShaderHost::createDataTexture(const float* data, int width, int height) {
    GLuint texture;
    glGenTextures(1, &texture);
    glBindTexture(GL_TEXTURE_2D, texture);

    // Use R32F for single-channel float data
    glTexImage2D(GL_TEXTURE_2D, 0, GL_R32F, width, height, 0, GL_RED, GL_FLOAT, data);

    // Bilinear filtering for smooth gradients
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

    return texture;
}
