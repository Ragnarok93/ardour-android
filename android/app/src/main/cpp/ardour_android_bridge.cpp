#include <jni.h>

#include <string>

#include "android_audio_driver.h"

namespace {
constexpr jint kEngineApiVersion = 2;

jstring toJavaString(JNIEnv* env, const std::string& value) {
    return env->NewStringUTF(value.c_str());
}
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_ardour_android_engine_ArdourNative_bootstrapState(
        JNIEnv* env,
        jclass) {
    return env->NewStringUTF(
        "Android native bridge + Oboe ready; libardour linkage pending"
    );
}

extern "C" JNIEXPORT jint JNICALL
Java_org_ardour_android_engine_ArdourNative_engineApiVersion(
        JNIEnv*,
        jclass) {
    return kEngineApiVersion;
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_ardour_android_engine_ArdourNative_audioProbeState(
        JNIEnv* env,
        jclass) {
    return toJavaString(env, ardour::android::audioDriver().statusText());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_org_ardour_android_engine_ArdourNative_startAudioProbe(
        JNIEnv*,
        jclass) {
    return ardour::android::audioDriver().start() ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_org_ardour_android_engine_ArdourNative_stopAudioProbe(
        JNIEnv*,
        jclass) {
    ardour::android::audioDriver().stop();
}
