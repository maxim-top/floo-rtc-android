package top.maxim.rtc.webrtc

import org.webrtc.*

/**
 * Description : PeerConnection通道封装，包括PeerConnection创建及状态回调
 * Created by Mango on 10/23/21.
 */
class Peer(factory: PeerConnectionFactory, rtcConfig: PeerConnection.RTCConfiguration) : SdpObserver, PeerConnection.Observer {

    var mPeerConnection : PeerConnection? = null

    var mRoomId : String ?= null
    
    private var createOffer: Boolean = false

    private var mOfferCreate : ((sdp : SessionDescription) -> Unit)? = null
    private var mAnswerCreate : ((sdp : SessionDescription) -> Unit)? = null
    private var mVideoTrack : VideoTrack? = null
    private var mAudioTrack : AudioTrack? = null

    init {
        mPeerConnection = factory.createPeerConnection(rtcConfig, this)
    }

    /**
     * 自己发起offer
     */
    fun createOffer(constraints : MediaConstraints, roomId : String?, success : (sdp : SessionDescription) -> Unit){
        mRoomId = roomId
        mOfferCreate = success
        createOffer = true
        mPeerConnection?.createOffer(this, constraints)
    }

    /**
     * 收到别人的sdp 新增answer
     */
    fun createAnswer(sdp : SessionDescription, constraints : MediaConstraints, roomId : String?, success : (sdp : SessionDescription) -> Unit){
        mRoomId = roomId
        mAnswerCreate = success
        createOffer = false
        mPeerConnection?.setRemoteDescription(this, sdp)
        mPeerConnection?.createAnswer(this, constraints)
    }

    /**
     * 收到自己的answer
     */
    fun receiveAnswer(sdp : SessionDescription, constraints : MediaConstraints){
        mPeerConnection?.setRemoteDescription(this, sdp)
    }

    /**
     * 开启对方remote
     */
    fun setRemote(remoteAdd : (videoTrack: VideoTrack?, audioTrack: AudioTrack?) -> Unit){
        remoteAdd(mVideoTrack, mAudioTrack)
    }

    /**SdpObserver是来回调sdp是否创建(offer,answer)成功，是否设置描述成功(local,remote）的接口**/
    override fun onCreateSuccess(sdp: SessionDescription) {
        mPeerConnection?.setLocalDescription(this, sdp)
        if (createOffer) {
            mOfferCreate?.let { it(sdp) }
        } else {
            mAnswerCreate?.let { it(sdp) }
        }
    }

    //Set{Local,Remote}Description()成功回调
    override fun onSetSuccess() {
        
    }

    override fun onCreateFailure(p0: String?) {
        
    }

    override fun onSetFailure(p0: String?) {
        
    }

    //信令状态改变时候触发
    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
        
    }

    //IceConnectionState连接状态改变时候触发
    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
        
    }

    //IceConnectionState连接接收状态改变
    override fun onIceConnectionReceivingChange(p0: Boolean) {
        
    }

    //IceConnectionState网络信息获取状态改变
    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
        
    }

    //新ice地址被找到触发
    override fun onIceCandidate(p0: IceCandidate?) {
    }

    //ice地址被移除掉触发
    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
        
    }

    override fun onAddStream(p0: MediaStream?) {
        
    }

    override fun onRemoveStream(p0: MediaStream?) {
        
    }

    //Peer连接远端开启数据传输通道时触发
    override fun onDataChannel(p0: DataChannel?) {
        
    }

    //通道交互协议需要重新协商时触发
    override fun onRenegotiationNeeded() {
        
    }

    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
    }

    override fun onTrack(transceiver: RtpTransceiver?) {
        super.onTrack(transceiver)
        val track = transceiver?.receiver?.track()
        track?.let {
            when(it){
                is VideoTrack -> mVideoTrack = it
                is AudioTrack -> mAudioTrack = it
            }
        }
    }
}