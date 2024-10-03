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

#include "yolo.h"

#include "ndkcamera.h"

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#if __ARM_NEON
#include <arm_neon.h>
#endif // __ARM_NEON

static int draw_unsupported(cv::Mat &rgb) {
    const char text[] = "unsupported";

    int baseLine = 0;
    cv::Size label_size = cv::getTextSize(text, cv::FONT_HERSHEY_SIMPLEX, 1.0, 1, &baseLine);

    int y = (rgb.rows - label_size.height) / 2;
    int x = (rgb.cols - label_size.width) / 2;

    cv::rectangle(rgb, cv::Rect(cv::Point(x, y),
                                cv::Size(label_size.width, label_size.height + baseLine)),
                  cv::Scalar(255, 255, 255), -1);

    cv::putText(rgb, text, cv::Point(x, y + label_size.height),
                cv::FONT_HERSHEY_SIMPLEX, 1.0, cv::Scalar(0, 0, 0));

    return 0;
}

static int draw_fps(cv::Mat &rgb) {
    // resolve moving average
    float avg_fps = 0.f;
    {
        static double t0 = 0.f;
        static float fps_history[10] = {0.f};

        double t1 = ncnn::get_current_time();
        if (t0 == 0.f) {
            t0 = t1;
            return 0;
        }

        float fps = 1000.f / (t1 - t0);
        t0 = t1;

        for (int i = 9; i >= 1; i--) {
            fps_history[i] = fps_history[i - 1];
        }
        fps_history[0] = fps;

        if (fps_history[9] == 0.f) {
            return 0;
        }

        for (int i = 0; i < 10; i++) {
            avg_fps += fps_history[i];
        }
        avg_fps /= 10.f;
    }

    char text[32];
    sprintf(text, "FPS=%.2f", avg_fps);

    int baseLine = 0;
    cv::Size label_size = cv::getTextSize(text, cv::FONT_HERSHEY_SIMPLEX, 0.5, 1, &baseLine);

    int y = 0;
    int x = rgb.cols - label_size.width;

    cv::rectangle(rgb, cv::Rect(cv::Point(x, y),
                                cv::Size(label_size.width, label_size.height + baseLine)),
                  cv::Scalar(255, 255, 255), -1);

    cv::putText(rgb, text, cv::Point(x, y + label_size.height),
                cv::FONT_HERSHEY_SIMPLEX, 0.5, cv::Scalar(0, 0, 0));

    return 0;
}

static Yolo *g_yolo = 0;
static ncnn::Mutex lock;

class MyNdkCamera : public NdkCameraWindow {
public:
    virtual void on_image_render(cv::Mat &rgb) const;
};

void MyNdkCamera::on_image_render(cv::Mat &rgb) const {
    // nanodet
    {
        ncnn::MutexLockGuard g(lock);

        if (g_yolo) {
            std::vector<Object> objects;
            g_yolo->detect(rgb, objects);

            g_yolo->draw(rgb, objects);
        } else {
            draw_unsupported(rgb);
        }
    }

    draw_fps(rgb);
}

static MyNdkCamera *g_camera = 0;

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "JNI_OnLoad");

    g_camera = new MyNdkCamera;

    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "JNI_OnUnload");

    {
        ncnn::MutexLockGuard g(lock);

        delete g_yolo;
        g_yolo = 0;
    }

    delete g_camera;
    g_camera = 0;
}

// public native boolean loadModel(AssetManager mgr, int modelid, int cpugpu);
JNIEXPORT jboolean JNICALL
Java_com_feyiuremote_libs_AI_detectors_YoloV8_Yolov8Ncnn_loadModel(JNIEnv *env, jobject thiz,
                                                                   jobject assetManager,
                                                                   jstring model_fname,
                                                                   jint cpugpu) {
    if (cpugpu < 0 || cpugpu > 1) {
        return JNI_FALSE;
    }

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn-native", "Trying to load Yolov8 model...");

    AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);
    jboolean iscopy;
    const char *model_fname_chars = (*env).GetStringUTFChars(model_fname, &iscopy);

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn-native", "Loading YoloV8 model: %s",
                        model_fname_chars);

    const float mean_vals[3] = {103.53f, 116.28f, 123.675f};

    const float norm_vals[3] = {1 / 255.f, 1 / 255.f, 1 / 255.f};

    int target_size = 320;
    bool use_gpu = (int) cpugpu == 1;

    // reload
    {
        ncnn::MutexLockGuard g(lock);

        if (use_gpu && ncnn::get_gpu_count() == 0) {
            // no gpu
            delete g_yolo;
            g_yolo = 0;
        } else {
            if (!g_yolo)
                g_yolo = new Yolo;
            g_yolo->load(mgr, model_fname_chars, target_size, mean_vals, norm_vals, use_gpu);
        }
    }

    return JNI_TRUE;
}

jobject createBitmap(JNIEnv *env, int width, int height) {
    jclass bitmapConfigClass = env->FindClass("android/graphics/Bitmap$Config");
    jfieldID argb8888FieldID = env->GetStaticFieldID(bitmapConfigClass, "ARGB_8888",
                                                     "Landroid/graphics/Bitmap$Config;");
    jobject argb8888Config = env->GetStaticObjectField(bitmapConfigClass, argb8888FieldID);

    jclass bitmapClass = env->FindClass("android/graphics/Bitmap");
    jmethodID createBitmapMethodID = env->GetStaticMethodID(bitmapClass, "createBitmap",
                                                            "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    jobject bitmap = env->CallStaticObjectMethod(bitmapClass, createBitmapMethodID, width, height,
                                                 argb8888Config);

    return bitmap;
}

//JNIEXPORT jobject JNICALL
//AI.detectors.YoloV8.Yolov8Ncnn.draw(JNIEnv *env, jobject instance, jlong matAddr) {
//    {
//        g_yolo->draw(rgb, objects);
//
//
//        // Create a new Bitmap in native code
//        cv::cvtColor(rgb, rgb, cv::COLOR_RGB2RGBA);
//        jobject bitmap = createBitmap(env, rgb.cols, rgb.rows);
//        AndroidBitmapInfo info;
//        void *pixels;
//
//        if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
//        // Handle error
//        return nullptr;
//        }
//
//        if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
//        // Handle error
//        return nullptr;
//        }
//
//        // Convert the Mat to RGBA format
//        //            cv::cvtColor(rgb, rgb, cv::COLOR_BGR2RGBA);
//
//        // Copy the data from Mat to the bitmap
//        memcpy(pixels, rgb.data, rgb.total() * rgb.elemSize());
//
//        // Unlock the bitmap
//        AndroidBitmap_unlockPixels(env, bitmap);
//
//        return bitmap;
//    }
//}

jobject convertRectToJavaRect(JNIEnv *env, const cv::Rect &rect) {
    jclass rectClass = env->FindClass("org/opencv/core/Rect");
    jmethodID rectConstructor = env->GetMethodID(rectClass, "<init>", "(IIII)V");

    // Create Java Rect object
    jobject rectObject = env->NewObject(rectClass, rectConstructor,
                                        rect.x, rect.y, rect.width, rect.height);

    // Release local reference
    env->DeleteLocalRef(rectClass);

    return rectObject;
}

// Function to convert cv::Mat to Java Mat
jobject convertMatToJavaMat(JNIEnv *env, const cv::Mat &mat) {
    // Assuming the org.opencv.core.CvType class is available
    jclass matClass = env->FindClass("org/opencv/core/Mat");
    // Find the constructor with the appropriate signature
    jmethodID matConstructor = env->GetMethodID(matClass, "<init>", "(IIILjava/nio/ByteBuffer;)V");

    // Create Java ByteBuffer object from native data
    jobject byteBufferObject = env->NewDirectByteBuffer(mat.data, mat.total() * mat.elemSize());

    // Create Java Mat object
    jobject matObject = env->NewObject(matClass, matConstructor,
                                       mat.rows, mat.cols, mat.type(), byteBufferObject);

    // Release local reference
    env->DeleteLocalRef(matClass);

    return matObject;
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_feyiuremote_libs_AI_detectors_YoloV8_Yolov8Ncnn_detect(JNIEnv *env, jobject instance,
                                                                jlong matAddr) {
    jclass detectedObjectClass = env->FindClass(
            "com/feyiuremote/libs/AI/detectors/YoloV8/DetectedObject");
    jmethodID detectedObjectConstructor = env->GetMethodID(detectedObjectClass, "<init>",
                                                           "(ILorg/opencv/core/Rect;Lorg/opencv/core/Mat;)V");
    // nanodet
    {
        cv::Mat &rgb = *(cv::Mat *) matAddr;
        ncnn::MutexLockGuard g(lock);

        // Print matrix size
        int rows = rgb.rows;
        int cols = rgb.cols;
        __android_log_print(ANDROID_LOG_DEBUG, "ncnn-native", "Matrix Size: %d x %d", rows, cols);


        if (g_yolo) {
            std::vector<Object> objects;
            g_yolo->detect(rgb, objects, 0.3f, 0.3f);

            // Convert C++ objects vector to Java DetectedObject array



            jobjectArray detectedObjectsArray = env->NewObjectArray(objects.size(),
                                                                    detectedObjectClass, nullptr);

            for (int i = 0; i < objects.size(); i++) {
                // Convert objects[i] to Java objects
                jobject rectObject = convertRectToJavaRect(env, objects[i].rect);
                jobject maskObject = convertMatToJavaMat(env, objects[i].mask);

                // Create DetectedObject instance
                jobject detectedObject = env->NewObject(detectedObjectClass, detectedObjectConstructor,
                                                        objects[i].label, rectObject, maskObject);

                // Set the DetectedObject in the array
                env->SetObjectArrayElement(detectedObjectsArray, i, detectedObject);

                // Release local references
                env->DeleteLocalRef(rectObject);
                env->DeleteLocalRef(maskObject);
                env->DeleteLocalRef(detectedObject);

                __android_log_print(ANDROID_LOG_DEBUG, "ncnn-native", "Convert successful");
            }

            return detectedObjectsArray;
        }

        return nullptr;
    }
}

// public native boolean openCamera(int facing);
JNIEXPORT jboolean JNICALL
Java_com_tencent_yolov8ncnn_Yolov8Ncnn_openCamera(JNIEnv *env, jobject thiz, jint facing) {
    if (facing < 0 || facing > 1)
        return JNI_FALSE;

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "openCamera %d", facing);

    g_camera->open((int) facing);

    return JNI_TRUE;
}

// public native boolean closeCamera();
JNIEXPORT jboolean JNICALL
Java_com_tencent_yolov8ncnn_Yolov8Ncnn_closeCamera(JNIEnv *env, jobject thiz) {
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "closeCamera");

    g_camera->close();

    return JNI_TRUE;
}

// public native boolean setOutputWindow(Surface surface);
// TODO: Fix app path
JNIEXPORT jboolean JNICALL
Java_com_tencent_yolov8ncnn_Yolov8Ncnn_setOutputWindow(JNIEnv *env, jobject thiz, jobject surface) {
    ANativeWindow *win = ANativeWindow_fromSurface(env, surface);

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "setOutputWindow %p", win);

    g_camera->set_window(win);

    return JNI_TRUE;
}

}
