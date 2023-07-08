package top.maxim.rtc.webrtc

import android.content.Context
import android.media.AudioManager
import android.text.TextUtils
import android.util.Log
import org.webrtc.*
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule.AudioRecordErrorCallback
import org.webrtc.audio.JavaAudioDeviceModule.AudioTrackErrorCallback
import top.maxim.rtc.view.CoreRTCRenterView


/**
 * Description : webRTC
 * Created by Mango on 10/23/21.
 */
class WebRtcClient {

    private val TAG = "WebRtcClient"

    private var mStrategy: RTCParametersStrategy = RTCParametersStrategy.Builder().build()

    private val mPeerFactory : PeerConnectionFactory

    private val mPeerConfig : PeerConnection.RTCConfiguration

    private val mSdpConstraints : MediaConstraints

    private val mEglBase : EglBase

    private val mPeers  = mutableMapOf<String, Peer>()

    private var myPeer: Peer? = null

    private var mTrack: LocalTrack

    private var mLocalView : CoreRTCRenterView? = null

    init {
        mPeerFactory = createPeerFactory()
        mPeerConfig = createPeerConfig()
        mSdpConstraints = createMediaConstraints()
        mEglBase = EglBase.create()
        mTrack = LocalTrack(mPeerFactory)
    }

    fun setStrategy(strategy: RTCParametersStrategy){
        mStrategy = strategy
    }

    fun getStrategy(): RTCParametersStrategy{
        return mStrategy
    }

    private fun createPeerFactory() : PeerConnectionFactory{
        //创建webRtc连接工厂类
        val encoderFactory: VideoEncoderFactory
        val decoderFactory: VideoDecoderFactory
        val enableH264HighProfile = "H264 High" == mStrategy.videoCodec
        //编解码模式【硬件加速，软编码】
        if (mStrategy.videoCodecHwAcceleration) {
            encoderFactory = DefaultVideoEncoderFactory(
                    mEglBase.eglBaseContext, true /* enableIntelVp8Encoder */, enableH264HighProfile)
            decoderFactory = DefaultVideoDecoderFactory(mEglBase.eglBaseContext)
        } else {
            encoderFactory = SoftwareVideoEncoderFactory()
            decoderFactory = SoftwareVideoDecoderFactory()
        }
        //音频模式
        val audioDeviceModule = createJavaAudioDevice()
        //构建PeerConnectionFactory
        val options = PeerConnectionFactory.Options()
        PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(ContextUtils.getApplicationContext())
                        .setEnableInternalTracer(true)
                        .createInitializationOptions())
        return PeerConnectionFactory
                .builder()
                .setOptions(options)
                .setAudioDeviceModule(audioDeviceModule)
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory()
    }

    private fun createIceServers() : List<PeerConnection.IceServer> {
        val servers = mutableListOf<PeerConnection.IceServer>()
        servers.add(PeerConnection.IceServer.builder("stun:stun.xten.com").createIceServer())
        return servers
    }

    //创建mPeerConfiguration参数
    private fun createPeerConfig() : PeerConnection.RTCConfiguration{
        val peerConfig = PeerConnection.RTCConfiguration(createIceServers())
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        with(peerConfig){
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            // Use ECDSA encryption.
            keyType = PeerConnection.KeyType.ECDSA
            // Enable DTLS for normal calls and disable for loopback calls.
//            enableDtlsSrtp = !pcParams.loopback
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        return peerConfig
    }

    private fun createMediaConstraints() : MediaConstraints {
        val constraints = MediaConstraints()

        //回声消除
        constraints.mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
        //自动增益
        constraints.mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
        //高音过滤
        constraints.mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
        //噪音处理
        constraints.mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
        constraints.mandatory.add(
                MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        constraints.mandatory.add(MediaConstraints.KeyValuePair(
                "OfferToReceiveVideo", "true"))
        constraints.optional.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
        return constraints
    }

    private fun createJavaAudioDevice(): AudioDeviceModule {
        // Set audio record error callbacks.
        val audioRecordErrorCallback: AudioRecordErrorCallback = object : AudioRecordErrorCallback {
            override fun onWebRtcAudioRecordInitError(errorMessage: String) {
                Log.e(TAG, "onWebRtcAudioRecordInitError: $errorMessage")
            }

            override fun onWebRtcAudioRecordStartError(
                    errorCode: JavaAudioDeviceModule.AudioRecordStartErrorCode, errorMessage: String) {
                Log.e(TAG, "onWebRtcAudioRecordStartError: $errorCode. $errorMessage")
            }

            override fun onWebRtcAudioRecordError(errorMessage: String) {
                Log.e(TAG, "onWebRtcAudioRecordError: $errorMessage")
            }
        }
        val audioTrackErrorCallback: AudioTrackErrorCallback = object : AudioTrackErrorCallback {
            override fun onWebRtcAudioTrackInitError(errorMessage: String) {
                Log.e(TAG, "onWebRtcAudioTrackInitError: $errorMessage")
            }

            override fun onWebRtcAudioTrackStartError(
                    errorCode: JavaAudioDeviceModule.AudioTrackStartErrorCode, errorMessage: String) {
                Log.e(TAG, "onWebRtcAudioTrackStartError: $errorCode. $errorMessage")
            }

            override fun onWebRtcAudioTrackError(errorMessage: String) {
                Log.e(TAG, "onWebRtcAudioTrackError: $errorMessage")
            }
        }
        return JavaAudioDeviceModule.builder(ContextUtils.getApplicationContext())
                .setAudioRecordErrorCallback(audioRecordErrorCallback)
                .setAudioTrackErrorCallback(audioTrackErrorCallback)
                .createAudioDeviceModule()
    }

    //关联本地video
    fun startLocalPreView(localRender: CoreRTCRenterView, hasVideo : Boolean) {
        mLocalView = localRender
        val audioTrack = mTrack.mAudioTrack
        val videoTrack = mTrack.mVideoTrack
        if(hasVideo){
            videoTrack?.let {
                localRender.addVideoTrack(mEglBase, it)
            }
        }
    }

    //取消本地video
    fun stopLocalPreView() {
        mTrack.stopCapture()
    }

    //关联远端video
    fun startRemotePreView(remoteRender: CoreRTCRenterView, hasVideo: Boolean, audioTrack: AudioTrack?, videoTrack: VideoTrack?) {
        if(hasVideo){
            videoTrack?.let {
                remoteRender.addVideoTrack(mEglBase, it)
            }
        }
    }

    //以自己的peer对象发起创建offer
    fun createOffer(roomId: String?, video: Boolean, audio: Boolean, success: (sdp: SessionDescription) -> Unit) {
        myPeer = Peer(mPeerFactory, mPeerConfig)
        if (video) {
            val videoTrack = mTrack.createLocalVideoTrack(mEglBase, mStrategy.videoWidth, mStrategy.videoHeight, mStrategy.videoFps)
            myPeer?.mPeerConnection?.addTrack(videoTrack)
        }
        if (audio) {
            val audioTrack = mTrack.createLocalAudioTrack()
            myPeer?.mPeerConnection?.addTrack(audioTrack)
        }
        myPeer?.createOffer(mSdpConstraints, roomId, success)
    }

    //发布成功  以自己的peer对象进行回复  创建answer
    fun receiveAnswer(sessionDescription: SessionDescription){
        myPeer?.receiveAnswer(sessionDescription, mSdpConstraints)
    }

    //收到远端的offer 以远端的peer对象进行answer
    fun createAnswer(roomId: String?, userId: String?, sessionDescription: SessionDescription, success: (sdp: SessionDescription) -> Unit){
        val peer = getPeer(userId)
        peer?.createAnswer(sessionDescription, mSdpConstraints, roomId, success)
    }

    fun setRemote(userId : String, remoteAdd : (videoTrack: VideoTrack?, audioTrack: AudioTrack?) -> Unit){
        val peer = getPeer(userId)
        peer?.setRemote(remoteAdd)
    }

    /**
     * 切换摄像头
     */
    fun switchCamera() {
        mTrack.switchCamera({ front ->
            if (front) mLocalView?.setMirror(true) else mLocalView?.setMirror(false)
        }, { error ->

        })
    }

    /**
     * 切换扬声器  麦克风
     */
    val audioManager = ContextUtils.getApplicationContext()?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    fun switchSpeaker(speaker: Boolean) {
        if (speaker) {
            // 打开扬声器
            audioManager.isSpeakerphoneOn = true
            audioManager.mode = AudioManager.MODE_NORMAL
        } else {
            // 关闭扬声器，从听筒，耳机中选择设备播放
            audioManager.isSpeakerphoneOn = false
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        }
    }

    private fun getPeer(id: String?) : Peer?{
        if (TextUtils.isEmpty(id)) {
            return null
        }
        var peer: Peer? = mPeers[id]
        if (peer == null) {
            peer = Peer(mPeerFactory, mPeerConfig)
            mPeers[id!!] = peer
        }
        return peer
    }

    fun muteLocalVideo(mute: Boolean){
        mTrack.mVideoTrack?.setEnabled(mute)
    }

    fun muteLocalAudio(mute: Boolean){
        mTrack.mAudioTrack?.setEnabled(mute)
    }

    fun releaseRTC(){
        mTrack.dispose()
        myPeer = null
        mPeers.clear()
    }
}