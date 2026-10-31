package xyz.nextalone.nagram.helper

import org.webrtc.RendererCommon
import org.webrtc.TextureViewRenderer
import xyz.nextalone.nagram.NaConfig

object CallVideoFitHelper {

    @JvmStatic
    fun isFullFrame(): Boolean =
        NaConfig.callVideoFitMode.Int() == NaConfig.CALL_VIDEO_FIT_FULL_FRAME

    @JvmStatic
    fun applyTo(renderer: TextureViewRenderer?) {
        renderer ?: return
        val fullFrame = isFullFrame()
        renderer.setScalingType(
            if (fullFrame) RendererCommon.ScalingType.SCALE_ASPECT_FIT else RendererCommon.ScalingType.SCALE_ASPECT_FILL
        )
        renderer.setSnapFillSuppressed(fullFrame)
    }

    @JvmStatic
    fun cycle(): Int {
        val next = (NaConfig.callVideoFitMode.Int() + 1) % NaConfig.CALL_VIDEO_FIT_MODE_COUNT
        NaConfig.callVideoFitMode.setConfigInt(next)
        return next
    }
}
