package top.maxim.rtc.view

import android.content.Context
import android.util.AttributeSet
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

/**
 * Description :
 * Created by Mango on 10/31/21.
 */
class CoreRTCRenterView @JvmOverloads constructor(context : Context, attrs : AttributeSet? = null) : SurfaceViewRenderer(context, attrs), RendererCommon.RendererEvents {

    private var isInited = false

    fun init(mEglBase : EglBase){
        super.init(mEglBase.eglBaseContext, null)
    }

    fun addVideoTrack(eglBase: EglBase?, track: VideoTrack?) {
        if (isInited) {
            return
        }
        eglBase?.let {
            isInited = true
            init(it)
            track?.addSink(this)
        }
    }
}