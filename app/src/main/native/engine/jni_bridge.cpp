#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "AkariaEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_akaria_agent_AkariaEngine_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from Akaria Native Engine! C++ is ready to load GGUF.";
    return env->NewStringUTF(hello.c_str());
}
