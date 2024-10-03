cmake_minimum_required(VERSION 3.10)

project(yolov8ncnn)

set(OpenCV_DIR ${CMAKE_SOURCE_DIR}/opencv-mobile-4.8.1-android/sdk/native/jni)
#find_package(OpenCV REQUIRED)
find_package(OpenCV REQUIRED core imgproc)

set(ncnn_DIR ${CMAKE_SOURCE_DIR}/ncnn-20231027-android-vulkan/${ANDROID_ABI}/lib/cmake/ncnn)
find_package(ncnn REQUIRED)

set(YOLO_SOURCE_FILES
        yolov8/yolov8ncnn.cpp
        yolov8/yolo.cpp
        yolov8/ndkcamera.cpp
)

add_library(yolov8ncnn SHARED ${YOLO_SOURCE_FILES})

set(NANODET_SOURCE_FILES
        ncnn-bodypose/nanodet.cpp
        ncnn-bodypose/nanodetncnn.cpp
)

add_library(ncnnbodypose SHARED ${NANODET_SOURCE_FILES})

target_link_libraries(yolov8ncnn ncnn ${OpenCV_LIBS} camera2ndk mediandk)
target_link_libraries(ncnnbodypose ncnn ${OpenCV_LIBS} camera2ndk mediandk)


add_library(crclib-c SHARED crclib-c.cpp)