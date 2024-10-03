// Tencent is pleased to support the open source community by making ncnn available.
//
// Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
//
// Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the License at
//
// https://opensource.org/licenses/BSD-3-Clause
//
// Unless required by applicable law or agreed to in writing, software distributed
// under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// CONDITIONS OF ANY KIND, either express or implied. See the License for the
// specific language governing permissions and limitations under the License.

#include <android/asset_manager_jni.h>
#include <android/native_window_jni.h>
#include <android/native_window.h>

#include <android/log.h>

#include <jni.h>

#include <string>
#include <vector>

#include <platform.h>
#include <benchmark.h>

#include "nanodet.h"

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#if __ARM_NEON
#include <arm_neon.h>
#endif // __ARM_NEON

static NanoDet *g_nanodet = 0;
static ncnn::Mutex lock;

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "JNI_OnLoad");

    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "JNI_OnUnload");

    {
        ncnn::MutexLockGuard g(lock);

        delete g_nanodet;
        g_nanodet = 0;
    }
}


extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_feyiuremote_libs_AI_detectors_Nanodet_NanodetNcnn_detect(JNIEnv *env, jobject thiz,
                                                                  jlong matAddr) {
    // TODO: implement detect()
    jclass detectedObjectClass = env->FindClass(
            "com/feyiuremote/libs/AI/detectors/Nanodet/DetectedPose");
    jmethodID detectedObjectConstructor = env->GetMethodID(detectedObjectClass, "<init>",
                                                           "(FFFFFF)V");
    // nanodet
    {
        cv::Mat &rgb = *(cv::Mat *) matAddr;
        ncnn::MutexLockGuard g(lock);

        if (g_nanodet) {
            std::vector<Person> objects;

            g_nanodet->detect(rgb, objects);

            // Convert C++ objects vector to Java DetectedPose array



            jobjectArray detectedObjectsArray = env->NewObjectArray(objects.size(),
                                                                    detectedObjectClass, nullptr);

            int ii = 0;
            for (int i = 0; i < objects.size(); i++) {
                //Person person = objects[i];
//                for (int j = 0; j < 17; j++)
//                {
//                    if (person.points[j].prob > 0.2)
//                        cv::circle(rgb, cv::Point2f(person.points[j].x, person.points[j].y), 3, cv::Scalar(100, 255, 150), -1);
//
//                }
//                for (int j = 0; j < 18; j++)
//                {
//                    if (person.points[skele_index[j][0]].prob > 0.2 && person.points[skele_index[j][1]].prob > 0.2)
//                        cv::line(rgb, cv::Point(person.points[skele_index[j][0]].x, person.points[skele_index[j][0]].y),
//                                 cv::Point(person.points[skele_index[j][1]].x, person.points[skele_index[j][1]].y),
//                                 cv::Scalar(color_index[j][0], color_index[j][1], color_index[j][2]), 2);
//
//                }

                // Convert objects[i] to Java objects


                // Create DetectedPose instance

//                if (person.points[0].prob > 0.3) {
                    jobject detectedObject = env->NewObject(detectedObjectClass,
                                                            detectedObjectConstructor,
                                                            objects[i].points[0].x, objects[i].points[0].y,
                                                            objects[i].points[1].x, objects[i].points[1].y,
                                                            objects[i].points[2].x, objects[i].points[2].y
                    );

                    // Set the DetectedPose in the array
                    env->SetObjectArrayElement(detectedObjectsArray, ii++, detectedObject);
                    env->DeleteLocalRef(detectedObject);
//                }

                // Release local references
//                env->DeleteLocalRef(rectObject);
//                env->DeleteLocalRef(maskObject);


//                __android_log_print(ANDROID_LOG_DEBUG, "ncnn-native", "Convert successful");
            }

            return detectedObjectsArray;
        }
    }

    return nullptr;
}
}

// public native boolean loadModel(AssetManager mgr, int modelid, int cpugpu);
extern "C" JNIEXPORT jboolean JNICALL
Java_com_feyiuremote_libs_AI_detectors_Nanodet_NanodetNcnn_loadModel(JNIEnv *env, jobject thiz,
                                                                     jobject assetManager,
                                                                     jstring model_fname,
                                                                     jint target_size,
                                                                     jint cpugpu) {
    if (cpugpu < 0 || cpugpu > 1) {
        return JNI_FALSE;
    }

    AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);
    jboolean iscopy;
    const char *model_fname_chars = (*env).GetStringUTFChars(model_fname, &iscopy);

    __android_log_print(ANDROID_LOG_DEBUG, "nanodetncnn-native", "Loading model: %s",
                        model_fname_chars);

    const float mean_vals[3] = {127.5f, 127.5f, 127.5f};
    const float norm_vals[3] = {1 / 127.5f, 1 / 127.5f, 1 / 127.5f};

    float multipose_scale[5];

    // 192x256
    if (target_size == 192) {
        multipose_scale[0] = 0.02083333395421505;
        multipose_scale[1] = 0.015625;
        multipose_scale[2] = 0.02083333395421505;
        multipose_scale[3] = 0.015625;
        multipose_scale[4] = -0.069444440305233;
    } else if (target_size == 256) { //256x320
        multipose_scale[0] = 0.015625;
        multipose_scale[1] = 0.012500000186264515;
        multipose_scale[2] = 0.015625;
        multipose_scale[3] = 0.012500000186264515;
        multipose_scale[4] = -0.0520833320915699;
    }

    bool use_gpu = (int) cpugpu == 1;

    {
        ncnn::MutexLockGuard g(lock);

        if (use_gpu && ncnn::get_gpu_count() == 0) {
            // no gpu
            delete g_nanodet;
            g_nanodet = 0;
            __android_log_print(ANDROID_LOG_DEBUG, "nanodetncnn-native", "GPU is not available");
        } else {
            if (!g_nanodet)
                g_nanodet = new NanoDet;

            g_nanodet->load(mgr, model_fname_chars, target_size, mean_vals, norm_vals, multipose_scale, use_gpu);
        }
    }

    return JNI_TRUE;
}