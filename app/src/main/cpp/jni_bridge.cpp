#include <jni.h>
#include "zarr_shader_host.h"
#include <android/log.h>

#define LOG_TAG "ZarrShaderJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// JNI function naming: Java_package_class_method
// Package: com.example.saltyoffshore.zarr
// Class: ZarrShaderHostBridge

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_example_saltyoffshore_zarr_ZarrShaderHostBridge_nativeCreate(
    JNIEnv* env, jobject thiz
) {
    auto* host = new ZarrShaderHost();
    LOGI("Created ZarrShaderHost: %p", host);
    return reinterpret_cast<jlong>(host);
}

JNIEXPORT void JNICALL
Java_com_example_saltyoffshore_zarr_ZarrShaderHostBridge_nativeDestroy(
    JNIEnv* env, jobject thiz, jlong handle
) {
    auto* host = reinterpret_cast<ZarrShaderHost*>(handle);
    if (host) {
        delete host;
        LOGI("Destroyed ZarrShaderHost");
    }
}

JNIEXPORT void JNICALL
Java_com_example_saltyoffshore_zarr_ZarrShaderHostBridge_nativeInitialize(
    JNIEnv* env, jobject thiz, jlong handle
) {
    auto* host = reinterpret_cast<ZarrShaderHost*>(handle);
    if (host) {
        host->initialize();
    }
}

JNIEXPORT void JNICALL
Java_com_example_saltyoffshore_zarr_ZarrShaderHostBridge_nativeRender(
    JNIEnv* env, jobject thiz, jlong handle,
    jdoubleArray projectionMatrix, jdouble zoom
) {
    auto* host = reinterpret_cast<ZarrShaderHost*>(handle);
    if (!host) return;

    jdouble* matrix = env->GetDoubleArrayElements(projectionMatrix, nullptr);
    host->render(matrix, zoom);
    env->ReleaseDoubleArrayElements(projectionMatrix, matrix, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_com_example_saltyoffshore_zarr_ZarrShaderHostBridge_nativeDeinitialize(
    JNIEnv* env, jobject thiz, jlong handle
) {
    auto* host = reinterpret_cast<ZarrShaderHost*>(handle);
    if (host) {
        host->deinitialize();
    }
}

JNIEXPORT void JNICALL
Java_com_example_saltyoffshore_zarr_ZarrShaderHostBridge_nativeUploadFrame(
    JNIEnv* env, jobject thiz, jlong handle,
    jstring entryId,
    jfloatArray floats,
    jint width, jint height,
    jdouble swEasting, jdouble swNorthing,
    jdouble neEasting, jdouble neNorthing,
    jfloat dataMin, jfloat dataMax
) {
    auto* host = reinterpret_cast<ZarrShaderHost*>(handle);
    if (!host) return;

    const char* entryIdCStr = env->GetStringUTFChars(entryId, nullptr);
    jfloat* floatData = env->GetFloatArrayElements(floats, nullptr);

    host->uploadFrame(
        entryIdCStr, floatData,
        width, height,
        swEasting, swNorthing, neEasting, neNorthing,
        dataMin, dataMax
    );

    env->ReleaseFloatArrayElements(floats, floatData, JNI_ABORT);
    env->ReleaseStringUTFChars(entryId, entryIdCStr);
}

JNIEXPORT jboolean JNICALL
Java_com_example_saltyoffshore_zarr_ZarrShaderHostBridge_nativeShowFrame(
    JNIEnv* env, jobject thiz, jlong handle, jstring entryId
) {
    auto* host = reinterpret_cast<ZarrShaderHost*>(handle);
    if (!host) return JNI_FALSE;

    const char* entryIdCStr = env->GetStringUTFChars(entryId, nullptr);
    bool result = host->showFrame(entryIdCStr);
    env->ReleaseStringUTFChars(entryId, entryIdCStr);

    return result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_example_saltyoffshore_zarr_ZarrShaderHostBridge_nativeSetColormap(
    JNIEnv* env, jobject thiz, jlong handle, jbyteArray rgbaBytes
) {
    auto* host = reinterpret_cast<ZarrShaderHost*>(handle);
    if (!host) return;

    jsize size = env->GetArrayLength(rgbaBytes);
    jbyte* bytes = env->GetByteArrayElements(rgbaBytes, nullptr);

    host->setColormap(reinterpret_cast<uint8_t*>(bytes), size);

    env->ReleaseByteArrayElements(rgbaBytes, bytes, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_com_example_saltyoffshore_zarr_ZarrShaderHostBridge_nativeSetUniforms(
    JNIEnv* env, jobject thiz, jlong handle,
    jfloat opacity,
    jfloat filterMin, jfloat filterMax,
    jint filterMode, jint scaleMode,
    jfloat blendFactor
) {
    auto* host = reinterpret_cast<ZarrShaderHost*>(handle);
    if (host) {
        host->setUniforms(opacity, filterMin, filterMax, filterMode, scaleMode, blendFactor);
    }
}

JNIEXPORT void JNICALL
Java_com_example_saltyoffshore_zarr_ZarrShaderHostBridge_nativeClearFrames(
    JNIEnv* env, jobject thiz, jlong handle
) {
    auto* host = reinterpret_cast<ZarrShaderHost*>(handle);
    if (host) {
        host->clearFrames();
    }
}

JNIEXPORT jboolean JNICALL
Java_com_example_saltyoffshore_zarr_ZarrShaderHostBridge_nativeIsLoaded(
    JNIEnv* env, jobject thiz, jlong handle, jstring entryId
) {
    auto* host = reinterpret_cast<ZarrShaderHost*>(handle);
    if (!host) return JNI_FALSE;

    const char* entryIdCStr = env->GetStringUTFChars(entryId, nullptr);
    bool result = host->isLoaded(entryIdCStr);
    env->ReleaseStringUTFChars(entryId, entryIdCStr);

    return result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_example_saltyoffshore_zarr_ZarrShaderHostBridge_nativeSetVisible(
    JNIEnv* env, jobject thiz, jlong handle, jboolean visible
) {
    auto* host = reinterpret_cast<ZarrShaderHost*>(handle);
    if (host) {
        host->setVisible(visible == JNI_TRUE);
    }
}

} // extern "C"
