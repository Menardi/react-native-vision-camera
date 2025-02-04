package com.mrousavy.camera.core.capture

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.util.Range
import com.mrousavy.camera.core.CameraDeviceDetails
import com.mrousavy.camera.core.InvalidFpsError
import com.mrousavy.camera.core.InvalidVideoStabilizationMode
import com.mrousavy.camera.core.PropRequiresFormatToBeNonNullError
import com.mrousavy.camera.core.outputs.SurfaceOutput
import com.mrousavy.camera.types.CameraDeviceFormat
import com.mrousavy.camera.types.Torch
import com.mrousavy.camera.types.VideoStabilizationMode

class RepeatingCaptureRequest(
  private val enableVideoPipeline: Boolean,
  torch: Torch = Torch.OFF,
  private val fps: Int? = null,
  private val videoStabilizationMode: VideoStabilizationMode = VideoStabilizationMode.OFF,
  enableVideoHdr: Boolean = false,
  enableLowLightBoost: Boolean = false,
  exposureBias: Double? = null,
  zoom: Float = 1.0f,
  format: CameraDeviceFormat? = null
) : CameraCaptureRequest(torch, enableVideoHdr, enableLowLightBoost, exposureBias, zoom, format) {
  override fun createCaptureRequest(
    device: CameraDevice,
    deviceDetails: CameraDeviceDetails,
    outputs: List<SurfaceOutput>
  ): CaptureRequest.Builder {
    val template = if (enableVideoPipeline) Template.RECORD else Template.PREVIEW
    return this.createCaptureRequest(template, device, deviceDetails, outputs)
  }

  private fun getBestDigitalStabilizationMode(deviceDetails: CameraDeviceDetails): Int {
    if (deviceDetails.digitalStabilizationModes.contains(CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION)) {
      return CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION
    }
    return CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_ON
  }

  override fun createCaptureRequest(
    template: Template,
    device: CameraDevice,
    deviceDetails: CameraDeviceDetails,
    outputs: List<SurfaceOutput>
  ): CaptureRequest.Builder {
    val builder = super.createCaptureRequest(template, device, deviceDetails, outputs)

    // Set FPS
    if (fps != null) {
      if (format == null) throw PropRequiresFormatToBeNonNullError("fps")
      if (format.maxFps < fps) throw InvalidFpsError(fps)
      builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(fps, fps))
    }

    // Set Video Stabilization
    if (videoStabilizationMode != VideoStabilizationMode.OFF) {
      if (format == null) throw PropRequiresFormatToBeNonNullError("videoStabilizationMode")
      if (!format.videoStabilizationModes.contains(videoStabilizationMode)) {
        throw InvalidVideoStabilizationMode(videoStabilizationMode)
      }
    }
    when (videoStabilizationMode) {
      VideoStabilizationMode.OFF -> {
        // do nothing
      }
      VideoStabilizationMode.STANDARD -> {
        builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, getBestDigitalStabilizationMode(deviceDetails))
      }
      VideoStabilizationMode.CINEMATIC, VideoStabilizationMode.CINEMATIC_EXTENDED -> {
        builder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON)
      }
    }

    return builder
  }
}
