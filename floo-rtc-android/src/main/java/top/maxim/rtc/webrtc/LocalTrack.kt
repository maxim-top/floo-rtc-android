package top.maxim.rtc.webrtc

import org.webrtc.*
/**
 * Description : 本地VideoTrack
 * Created by Mango on 10/31/21.
 */
class LocalTrack(private val mPeerFactory: PeerConnectionFactory) {

    private var cameraVideoCapturer: CameraVideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    private var isFront : Boolean = true

    var mVideoTrack : VideoTrack? = null

    private var mVideoSource: VideoSource? = null

    var mAudioTrack : AudioTrack? = null

    fun createLocalAudioTrack() : AudioTrack?{
        if(mAudioTrack != null){
            return mAudioTrack
        }
        val audioConstraints = MediaConstraints()
        val audioSource : AudioSource = mPeerFactory.createAudioSource(audioConstraints)
        mAudioTrack = mPeerFactory.createAudioTrack("audiotrack", audioSource)
        return mAudioTrack
    }

    fun createLocalVideoTrack(eglBase : EglBase, width: Int, height: Int, fps: Int) : VideoTrack?{
        if (mVideoSource == null) {
            mVideoSource = mPeerFactory.createVideoSource(false)
        }
        if (mVideoTrack == null) {
            mVideoTrack = mPeerFactory.createVideoTrack("ARDAMSv0", mVideoSource)
        }
        //创建VideoCapturer
        if(cameraVideoCapturer == null){
            var cameraname: String? = ""
            val cameraEnumerator = Camera2Enumerator(ContextUtils.getApplicationContext())
            val deviceNames = cameraEnumerator.deviceNames
            if (isFront) {
                //前置摄像头
                for (deviceName in deviceNames) {
                    if (cameraEnumerator.isFrontFacing(deviceName)) {
                        cameraname = deviceName
                    }
                }
            } else {
                //后置摄像头
                for (deviceName in deviceNames) {
                    if (cameraEnumerator.isBackFacing(deviceName)) {
                        cameraname = deviceName
                    }
                }
            }
            cameraVideoCapturer = cameraEnumerator.createCapturer(cameraname, null)
            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
            cameraVideoCapturer?.initialize(surfaceTextureHelper, ContextUtils.getApplicationContext(), mVideoSource?.capturerObserver)
            cameraVideoCapturer?.startCapture(width, height, fps)
            mVideoTrack?.setEnabled(true)
        } else {
            cameraVideoCapturer?.startCapture(width, height, fps)
        }
        return mVideoTrack
    }

    /**
     * 切换摄像头
     */
    fun switchCamera(success: (front: Boolean) -> Unit, failed: (error: String?) -> Unit) {
        surfaceTextureHelper?.setFrameRotation(90)
        cameraVideoCapturer?.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(front: Boolean) {
                isFront = front
                success(front)
            }

            override fun onCameraSwitchError(p0: String?) {
                failed(p0)
            }
        })
    }

    fun stopCapture(){
        cameraVideoCapturer?.stopCapture()
    }

    fun dispose(){
        cameraVideoCapturer?.let {
            with(it) {
                stopCapture()
                dispose()
            }
            cameraVideoCapturer = null
        }
        mVideoTrack?.let {
            it.dispose()
            mVideoTrack = null
        }
        mAudioTrack?.let {
            it.dispose()
            mAudioTrack = null
        }
    }
}