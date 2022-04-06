//
// Created by admin on 2022/3/29.
//

#ifndef APK_JIAGU_ANDROID_LOG_H
#define APK_JIAGU_ANDROID_LOG_H

#include <android/log.h>

// 定义一些宏，为了调用方便
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,__VA_ARGS__)
// If you want you can add other log definition for info, warning etc

/*// 日志输出级别
ANDROID_LOG_UNKNOWN = 0,
ANDROID_LOG_DEFAULT,    // only for SetMinPriority()
ANDROID_LOG_VERBOSE,
ANDROID_LOG_DEBUG,
ANDROID_LOG_INFO,
ANDROID_LOG_WARN,
ANDROID_LOG_ERROR,
ANDROID_LOG_FATAL,
ANDROID_LOG_SILENT
*/

// Demo
//static const char* TAG = "tag";
//const char* message = "msg";
//LOGE(TAG,"msg");
//LOGE(TAG,"%s",msg);

#endif
