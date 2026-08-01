#include <jni.h>
#include <string>
#include <android/log.h>
#include "llama.h"

#define LOG_TAG "AkariaEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_akaria_agent_AkariaEngine_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
        
    // Step 2: Initialize llama.cpp backend
    LOGI("Initializing llama.cpp backend...");
    llama_backend_init();
    LOGI("llama.cpp backend initialized successfully!");
    
    std::string hello = "llama_backend_init() SUCCESS! C++ engine is alive.";
    return env->NewStringUTF(hello.c_str());
}
