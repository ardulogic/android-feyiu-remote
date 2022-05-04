/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.feyiuremote.libs.AI.libs.mlkit.common.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build.VERSION_CODES;
import android.preference.PreferenceManager;

import com.feyiuremote.libs.AI.libs.mlkit.common.CameraSource;
import com.google.android.gms.common.images.Size;
import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.pose.PoseDetectorOptionsBase;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.camera.core.CameraSelector;

/** Utility class to retrieve shared preferences. */
public class PreferenceUtils {

  private static final int POSE_DETECTOR_PERFORMANCE_MODE_FAST = 1;

  static void saveString(Context context, @StringRes int prefKeyId, @Nullable String value) {
    PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putString(context.getString(prefKeyId), value)
        .apply();
  }

  @Nullable
  public static CameraSource.SizePair getCameraPreviewSizePair(Context context, int cameraId) {
    return new CameraSource.SizePair(
            Size.parseSize("640"),
            Size.parseSize("480")
    );
  }

  public static PoseDetectorOptionsBase getPoseDetectorOptionsForLivePreview(Context context) {
    int performanceMode = POSE_DETECTOR_PERFORMANCE_MODE_FAST;

    PoseDetectorOptions.Builder builder = new PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.STREAM_MODE);
    if (preferGPUForPoseDetection(context)) {
      builder.setPreferredHardwareConfigs(PoseDetectorOptions.CPU_GPU);
    }

    return builder.build();
  }

  public static boolean preferGPUForPoseDetection(Context context) {
    return false;
  }

  public static boolean shouldShowPoseDetectionInFrameLikelihoodLivePreview(Context context) {
    return true;
  }

  public static boolean shouldShowPoseDetectionInFrameLikelihoodStillImage(Context context) {
    return true;
  }

  public static boolean shouldPoseDetectionVisualizeZ(Context context) {
    return true;
  }

  public static boolean shouldPoseDetectionRescaleZForVisualization(Context context) {
    return true;
  }

  public static boolean shouldPoseDetectionRunClassification(Context context) {
    return false;
  }

  /**
   * Mode type preference is backed by {@link android.preference.ListPreference} which only support
   * storing its entry value as string type, so we need to retrieve as string and then convert to
   * integer.
   */
  private static int getModeTypePreferenceValue(
      Context context, @StringRes int prefKeyResId, int defaultValue) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    String prefKey = context.getString(prefKeyResId);
    return Integer.parseInt(sharedPreferences.getString(prefKey, String.valueOf(defaultValue)));
  }

  public static boolean isCameraLiveViewportEnabled(Context context) {
    return true;
  }

  private PreferenceUtils() {}
}
