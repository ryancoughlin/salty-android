# CMakeLists.txt for Zarr GPU Shader
# Builds OpenGL ES 3.0 CustomLayerHost for Mapbox Maps SDK

cmake_minimum_required(VERSION 3.22.1)
project("zarr-shader")

# C++17 for modern features
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Shared library for JNI
add_library(zarr-shader SHARED
    zarr_shader_host.cpp
    jni_bridge.cpp
)

# Find required libraries
find_library(log-lib log)
find_library(android-lib android)
find_library(gles-lib GLESv3)
find_library(egl-lib EGL)

# Link libraries
target_link_libraries(zarr-shader
    ${log-lib}
    ${android-lib}
    ${gles-lib}
    ${egl-lib}
)

# Include directories
target_include_directories(zarr-shader PRIVATE
    ${CMAKE_SOURCE_DIR}
)
