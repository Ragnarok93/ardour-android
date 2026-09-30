#include <jni.h>

namespace {
constexpr jint kEngineApiVersion = 1;
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_ardour_android_engine_ArdourNative_bootstrapState(
        JNIEnv* env,
        jclass) {
    return env->NewStringUTF(
        "Android native bridge ready; libardour linkage pending"
    );
}

extern "C" JNIEXPORT jint JNICALL
Java_org_ardour_android_engine_ArdourNative_engineApiVersion(
        JNIEnv*,
        jclass) {
    return kEngineApiVersion;
}
