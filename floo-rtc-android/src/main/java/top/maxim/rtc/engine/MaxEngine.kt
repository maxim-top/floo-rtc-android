package top.maxim.rtc.engine

import android.app.Application
import android.util.Log
import im.floo.floolib.*
import org.webrtc.*
import top.maxim.rtc.view.CoreRTCRenterView
import top.maxim.rtc.webrtc.RTCParametersStrategy
import top.maxim.rtc.webrtc.WebRtcClient
import java.lang.Thread

/**
 * Created by Mango on 10/23/21.
 */
class MaxEngine(mBMXClient: BMXClient) : BMXRTCEngine() {

    private val TAG = "MaxEngine"

    private val mStrategyBuilder: RTCParametersStrategy.Builder = RTCParametersStrategy.Builder()

    private var mClient : WebRtcClient? = WebRtcClient()

    private val mRtcService : BMXRTCService = mBMXClient.rtcManager

    private val mService : BMXRTCSignalService = mRtcService.bmxrtcSignalService

    private val mListener : BMXRTCSignalServiceListener

    private var mBMXRTCSession : BMXRTCSession? = null

    private var mPin : String? = null

    private var mRoomId : String? = null

    private var mUserId : Long? = null

    private val mEngineListeners : MutableSet<BMXRTCEngineListener> = mutableSetOf()

    companion object{
        fun init(application: Application){
            ContextUtils.initialize(application)
        }
    }

    init {
        mListener = object : BMXRTCSignalServiceListener(){
            override fun onSessionCreate(session: BMXRTCSession?, error: Int, reason: String?) {
                super.onSessionCreate(session, error, reason)
                if (!checkResult(error, reason, "onSessionCreate")) {
                    return
                }
                //session创建成功  then attach
                if (mBMXRTCSession != null){
                    mService.destroySession(mBMXRTCSession);
                }
                mBMXRTCSession = session
                mService.attachSession(session, BMXRTCSignalService.HandlerType.publishType)
            }

            override fun onSessionAttach(session: BMXRTCSession?, type: BMXRTCSignalService.HandlerType?, error: Int, reason: String?) {
                super.onSessionAttach(session, type, error, reason)
                if (!checkResult(error, reason, "onSessionAttach type is $type")) {
                    return
                }
                //session attach成功  then attach
                when(type){
                    BMXRTCSignalService.HandlerType.subscribeType -> {
                        //sub attach 之后 调用 判断房间是否存在
                        //加入房间  如果加入房间失败  需要判断是否是房间不存在需要创建
//                        if(mRoomId?.toLong() == mUserId){
//                            //需要创建房间
//                            mService.createRoom(session, BMXRTCSignalService.BMXRoomCreateOptions())
//                        } else{
//                        }
                        if (mRoomId.equals("0")){
                            val createOptions = BMXRTCSignalService.BMXRoomCreateOptions()
                            createOptions.mIsPermanent = false
                            createOptions.mRoomId = mRoomId?.toLong() ?: 0
                            createOptions.mPin = mPin;
                            mService.createRoom(session, createOptions)
                        } else {
                            mRoomId?.let { startPubJoin(it.toLong(), session) }
                        }
                    }
                    BMXRTCSignalService.HandlerType.publishType -> {
                        //Pub attach 之后 调用 sub attach
                        mService.attachSession(session, BMXRTCSignalService.HandlerType.subscribeType)
                    }
                }
            }

            override fun onPubJoinRoom(session: BMXRTCSession?, room: BMXRTCRoom?, publishers: BMXRTCPublishers?, error: Int, reason: String?) {
                super.onPubJoinRoom(session, room, publishers, error, reason)
                if (error == 426) {
                    //房间不存在  需要创建房间
                    mRoomId?.let {
                        val createOptions = BMXRTCSignalService.BMXRoomCreateOptions()
                        createOptions.mIsPermanent = false
                        createOptions.mRoomId = it.toLong()
                        createOptions.mPin = mPin
                        mService.createRoom(session, createOptions)
                    }
                    return
                }
                if (room != null) {
                    mRoomId = room.roomId().toLong().toString()
                }
                if (publishers?.size()!! > 0){
                    otherId = publishers.get(0)?.mUserId ?: 0
                }
                if (!checkResult(error, reason, "onPubJoinRoom")) {
                    onJoinRoom(error, reason, mRoomId)
                    return
                }
                onJoinRoom(error, reason, mRoomId)
                //开启订阅
                room?.let {
                    subscribeJoin(publishers, it.roomId())
                }
            }

            override fun onOtherPubJoinRoom(session: BMXRTCSession?, room: BMXRTCRoom?, publishers: BMXRTCPublishers?) {
                super.onOtherPubJoinRoom(session, room, publishers)
                //别人进入房间
                if (!checkResult(0, "", "onOtherPubJoinRoom")) {
                    return
                }
                if (publishers?.size()!! > 0){
                    otherId = publishers.get(0)?.mUserId ?: 0
                }
                //开启订阅
                room?.let {
                    subscribeJoin(publishers, it.roomId())
                }
            }

            override fun onPubConfigure(session: BMXRTCSession?, room: BMXRTCRoom?, sdp: BMXRoomSDPInfo?, streams: BMXRTCStreams?, error: Int, reason: String?) {
                super.onPubConfigure(session, room, sdp, streams, error, reason)
                if (!checkResult(error, reason, "onPubConfigure type is ${sdp?.type}")) {
                    return
                }
                //只能是自己收到的sdp 一定是answer 此时需要设置remote
                val sessionDescription = SessionDescription(SessionDescription.Type.ANSWER, sdp?.sdp)
                mClient?.receiveAnswer(sessionDescription)
                var hasVideo = false
                var hasAudio = false
                var hasData = false
                streams?.let {
                    if (!it.isEmpty) {
                        for (i in 0 until it.size()) {
                            val streamInfo = it[i.toInt()]
                            if (streamInfo.mType == "video") {
                                hasVideo = true
                            }
                            if (streamInfo.mType == "audio") {
                                hasAudio = true
                            }
                            if (streamInfo.mType == "data") {
                                hasData = true
                            }
                        }
                    }
                }
                onLocalPublish(buildBMXStreamInfo(mUserId.toString(), room?.roomId().toString(), hasVideo, hasAudio, hasData), reason, error)
            }

            override fun onPubUnPublish(session: BMXRTCSession?, room: BMXRTCRoom?, senderId: Int, error: Int, reason: String?) {
                super.onPubUnPublish(session, room, senderId, error, reason)
                if (!checkResult(error, reason, "onPubUnPublish")) {
                    return
                }
            }

            override fun onPublishWebrtcUp(session: BMXRTCSession?) {
                super.onPublishWebrtcUp(session)
                if (!checkResult(0, "", "onPublishWebrtcUp")) {
                    return
                }
            }

            override fun onSubJoinRoomUpdate(session: BMXRTCSession?, room: BMXRTCRoom?, sdp: BMXRoomSDPInfo?, senderId: Long, streams: BMXRTCStreams?, error: Int, reason: String?) {
                super.onSubJoinRoomUpdate(session, room, sdp, senderId, streams, error, reason)
                if (!checkResult(error, reason, "onSubJoinRoomUpdate")) {
                    return
                }
                if (mRoomId == null){
                    return
                }
                //收到别人的sdp  一定是offer 需要createAnswer
                val sessionDescription = SessionDescription(SessionDescription.Type.OFFER, sdp?.sdp)
                if (session != null) {
                    mClient?.createAnswer(room?.roomId().toString(), senderId.toString(), sessionDescription) { sdp1 ->
                        openSubscribe(sdp1, room!!)
                        var hasVideo = false
                        var hasAudio = false
                        var hasData = false
                        streams?.let {
                            if (!it.isEmpty) {
                                for (i in 0 until it.size()) {
                                    val streamInfo = it[i.toInt()]
                                    if (streamInfo.mType == "video") {
                                        hasVideo = true
                                    }
                                    if (streamInfo.mType == "audio") {
                                        hasAudio = true
                                    }
                                    if (streamInfo.mType == "data") {
                                        hasData = true
                                    }
                                }
                            }
                        }
                        onSubscribe(buildBMXStreamInfo(senderId.toString(), room.roomId().toString(), hasVideo, hasAudio, hasData), reason, error)
                    }
//                    mClient?.createAnswer(room?.roomId().toString(), senderId.toString(), sessionDescription, { sdp ->
//                        openSubscribe(sdp, room!!)
//                    }, { videoTrack, audioTrack ->
//                        var hasVideo = false
//                        var hasAudio = false
//                        var hasData = false
//                        streams?.let {
//                            if (!it.isEmpty) {
//                                for (i in 0 until it.size()) {
//                                    val streamInfo = it[i.toInt()]
//                                    if (streamInfo.mType == "video") {
//                                        hasVideo = true
//                                    }
//                                    if (streamInfo.mType == "audio") {
//                                        hasAudio = true
//                                    }
//                                    if (streamInfo.mType == "data") {
//                                        hasData = true
//                                    }
//                                }
//                            }
//                        }
//                        onSubscribe(buildBMXStreamInfo(senderId.toString(), room?.roomId().toString(), hasVideo, hasAudio, hasData, videoTrack = videoTrack, audioTrack = audioTrack), reason, error)
//                    })
                }
            }

            override fun onSubStart(session: BMXRTCSession?, room: BMXRTCRoom?, error: Int, reason: String?) {
                super.onSubStart(session, room, error, reason)
                if (!checkResult(error, reason, "onSubStart")) {
                    return
                }
            }

            override fun onSubPause(session: BMXRTCSession?, room: BMXRTCRoom?, error: Int, reason: String?) {
                super.onSubPause(session, room, error, reason)
                if (!checkResult(error, reason, "onSubPause")) {
                    return
                }
            }

            override fun onSubUnsubscribe(session: BMXRTCSession?, room: BMXRTCRoom?, streams: BMXRTCStreams?, error: Int, reason: String?) {
                super.onSubUnsubscribe(session, room, streams, error, reason)
                if (!checkResult(error, reason, "onSubUnsubscribe")) {
                    return
                }
            }

            override fun onSubConfigure(session: BMXRTCSession?, room: BMXRTCRoom?, error: Int, reason: String?) {
                super.onSubConfigure(session, room, error, reason)
                if (!checkResult(error, reason, "onSubConfigure")) {
                    return
                }
            }

            override fun onSubSwitch(session: BMXRTCSession?, room: BMXRTCRoom?, publisher: Long, error: Int, reason: String?) {
                super.onSubSwitch(session, room, publisher, error, reason)
                if (!checkResult(error, reason, "onSubSwitch")) {
                    return
                }
            }

            override fun onLeaveRoom(session: BMXRTCSession?, roomId: Long, senderId: Long, error: Int, reason: String?) {
                super.onLeaveRoom(session, roomId, senderId, error, reason)
                if (!checkResult(error, reason, "onLeaveRoom")) {
                    return
                }
                mService.detachSession(session, BMXRTCSignalService.HandlerType.publishType)
            }

            override fun onSubscribeWebrtcUp(session: BMXRTCSession?, senderId: Long) {
                super.onSubscribeWebrtcUp(session, senderId)
                if (!checkResult(0, "", "onSubscribeWebrtcUp")) {
                    return
                }
            }

            override fun onMediaInfo(session: BMXRTCSession?, senderId: Long, type: BMXTrackType?, receiving: Boolean, mid: String?) {
                super.onMediaInfo(session, senderId, type, receiving, mid)
                if (!checkResult(0, "", "onMediaInfo")) {
                    return
                }
            }

            override fun onSlowlink(session: BMXRTCSession?, senderId: Long, uplink: Boolean, nacks: Int) {
                super.onSlowlink(session, senderId, uplink, nacks)
                if (!checkResult(0, "", "onSlowlink")) {
                    return
                }
            }

            override fun onHangup(session: BMXRTCSession?, senderId: Long, reason: String?) {
                super.onHangup(session, senderId, reason)
                if (!checkResult(0, "", "onHangup")) {
                    return
                }
            }

            override fun onSessionHangup(session: BMXRTCSession?, error: Long, reason: String?) {
                super.onSessionHangup(session, error, reason)
                if (!checkResult(error.toInt(), reason, "onSessionHangup")) {
                    return
                }
            }

            override fun onSessionDetach(session: BMXRTCSession?, type: BMXRTCSignalService.HandlerType?, error: Int, reason: String?) {
                super.onSessionDetach(session, type, error, reason)
                if (!checkResult(error, reason, "onSessionDetach")) {
                    return
                }
                mService.destroySession(session)
            }

            override fun onSessionDestroy(sessionId: Long, error: Int, reason: String?) {
                super.onSessionDestroy(sessionId, error, reason)
                if (!checkResult(error, reason, "onSessionDestroy")) {
                    return
                }
            }

            override fun onRoomCreate(session: BMXRTCSession?, room: BMXRTCRoom?, error: Int, reason: String?) {
                super.onRoomCreate(session, room, error, reason)
                if (!checkResult(error, reason, "onRoomCreate")) {
                    return
                }
                //room创建成功之后调用发布
                room?.let {
                    startPubJoin(it.roomId(), session)
                }
            }

            override fun onRoomDestroy(session: BMXRTCSession?, roomId: Long, error: Int, reason: String?) {
                super.onRoomDestroy(session, roomId, error, reason)
                if (!checkResult(error, reason, "onRoomDestroy")) {
                    return
                }
            }

            override fun onRoomEdit(session: BMXRTCSession?, room: BMXRTCRoom?, error: Int, reason: String?) {
                super.onRoomEdit(session, room, error, reason)
                if (!checkResult(error, reason, "onRoomEdit")) {
                    return
                }
            }

            override fun onRoomExist(session: BMXRTCSession?, roomId: Long, exist: Boolean, error: Int, reason: String?) {
                super.onRoomExist(session, roomId, exist, error, reason)
                if (!checkResult(error, reason, "onRoomExist")) {
                    return
                }
            }

            override fun onRoomAllowed(session: BMXRTCSession?, room: BMXRTCRoom?, tokens: TagList?, error: Int, reason: String?) {
                super.onRoomAllowed(session, room, tokens, error, reason)
                if (!checkResult(error, reason, "onRoomAllowed")) {
                    return
                }
            }

            override fun onRoomKick(session: BMXRTCSession?, room: BMXRTCRoom?, userId: Long, error: Int, reason: String?) {
                super.onRoomKick(session, room, userId, error, reason)
                if (!checkResult(error, reason, "onRoomKick")) {
                    return
                }
            }

            override fun onRoomModerate(session: BMXRTCSession?, room: BMXRTCRoom?, userId: Long, error: Int, reason: String?) {
                super.onRoomModerate(session, room, userId, error, reason)
                if (!checkResult(error, reason, "onRoomModerate")) {
                    return
                }
            }

            override fun onRoomList(session: BMXRTCSession?, rooms: BMXRTCRooms?, error: Int, reason: String?) {
                super.onRoomList(session, rooms, error, reason)
                if (!checkResult(error, reason, "onRoomList")) {
                    return
                }
            }

            override fun onRoomListParticipants(session: BMXRTCSession?, room: BMXRTCRoom?, participants: BMXRTCRoomParticipants?, error: Int, reason: String?) {
                super.onRoomListParticipants(session, room, participants, error, reason)
                if (!checkResult(error, reason, "onRoomListParticipants")) {
                    return
                }
            }
        }
        mService.addBMXRTCSignalServiceListener(mListener)
        //设置默认videoConfig
        val config = BMXVideoConfig()
        config.profile = BMXVideoProfile.Profile_480_360
        setVideoProfile(config)
    }

    override fun addRTCEngineListener(listener: BMXRTCEngineListener?) {
        listener?.let {
            mEngineListeners.add(it)
        }
    }

    override fun removeRTCEngineListener(listener: BMXRTCEngineListener?) {
        listener?.let {
            mEngineListeners.remove(it)
        }
    }

    override fun setRoomType(type: BMXRoomType?): BMXErrorCode {
        return BMXErrorCode.NoError
    }

    override fun setVideoProfile(videoConfig: BMXVideoConfig?): BMXErrorCode {
        mStrategyBuilder.videoProfile(videoConfig)
        mClient?.setStrategy(mStrategyBuilder.build())
        return BMXErrorCode.NoError
    }

    override fun setAudioProfile(profile: BMXAudioProfile?): BMXErrorCode {
        return BMXErrorCode.NoError
    }

    override fun joinRoom(auth: BMXRoomAuth): BMXErrorCode {
        mClient = WebRtcClient()
        mUserId = auth.mUserId
        mRoomId = auth.mRoomId.toString()
        mPin    = auth.mToken
        mService.createSession()
        isOnCall = true
        return BMXErrorCode.NoError;
    }

    override fun leaveRoom(): BMXErrorCode {
        mRoomId = null
        mUserId = null
        mService.leaveRoom(mBMXRTCSession)
        mClient?.releaseRTC()
//        mService.destroySession(mBMXRTCSession)
        mBMXRTCSession = null
        isOnCall = false
        mClient = null
        return BMXErrorCode.NoError;
    }

    override fun publish(type: BMXVideoMediaType?, hasVideo: Boolean, hasAudio: Boolean): BMXErrorCode {
        mClient?.createOffer(mRoomId, hasVideo, hasAudio) { sdp ->
            val bmxSdp = BMXRoomSDPInfo()
            bmxSdp.sdp = sdp.description
            //获取自己createOffer获得的sdp  一定是offer
            bmxSdp.type = BMXRoomSDPType.Offer
            val width = mClient?.getStrategy()?.videoWidth
            val height = mClient?.getStrategy()?.videoHeight
            val opt = width?.let {
                height?.let { it1 ->
                    BMXRTCSignalService.BMXRoomPubConfigureOptions(
                        hasAudio, hasVideo, it, it1
                    )
                }
            }
            mService.pubConfigue(mBMXRTCSession, opt, bmxSdp)
        }
        return BMXErrorCode.NoError;
    }

    override fun unPublish(type: BMXVideoMediaType?): BMXErrorCode {
        return BMXErrorCode.NoError;
    }

    override fun subscribe(stream: BMXStream?): BMXErrorCode {
        return BMXErrorCode.NoError;
    }

    override fun unSubscribe(stream: BMXStream?): BMXErrorCode {
        return BMXErrorCode.NoError;
    }

    override fun startPreview(canvas: BMXVideoCanvas?): BMXErrorCode {
        if (canvas != null) {
            val view = canvas.mView as CoreRTCRenterView
            mClient?.startLocalPreView(view, canvas.mStream.mEnableVideo)
        }
        return BMXErrorCode.NoError;
    }

    override fun stopPreview(canvas: BMXVideoCanvas?): BMXErrorCode {
        mClient?.stopLocalPreView()
        return BMXErrorCode.NoError;
    }

    override fun startRemoteView(canvas: BMXVideoCanvas): BMXErrorCode {
        mClient?.setRemote(canvas.mUserId.toString()) { videoTrack, audioTrack ->
            mClient?.startRemotePreView(canvas.mView as CoreRTCRenterView, canvas.mStream.mEnableVideo, audioTrack, videoTrack)
        }
        return BMXErrorCode.NoError;
    }

    override fun stopRemoteView(canvas: BMXVideoCanvas?): BMXErrorCode {
        return BMXErrorCode.NoError;
    }

    override fun muteLocalAudio(mute: Boolean): BMXErrorCode {
        mClient?.muteLocalAudio(mute)
        return BMXErrorCode.NoError;
    }

    override fun muteLocalVideo(type: BMXVideoMediaType?, mute: Boolean): BMXErrorCode {
        mClient?.muteLocalVideo(mute)
        return BMXErrorCode.NoError;
    }

    override fun muteRemoteAudio(stream: BMXStream?, mute: Boolean): BMXErrorCode {
        return BMXErrorCode.NoError;
    }

    override fun muteRemoteVideo(stream: BMXStream?, mute: Boolean): BMXErrorCode {
        return BMXErrorCode.NoError;
    }

    override fun switchCamera(): BMXErrorCode {
        mClient?.switchCamera()
        return BMXErrorCode.NoError;
    }

    /**
     * 检查结果
     * @param error 结果
     */
    private fun checkResult(error: Int, reason: String?, message: String) : Boolean{
        val result = error == 0
        if (!result) {
            Log.e(TAG, "$message is fail, reason is $reason")
        } else {
            Log.e(TAG, "$message is success")
        }
        return result
    }

    /**
     * 发布进入
     * @param publishers 发布者列表
     */
    private fun subscribeJoin(publishers: BMXRTCPublishers?, roomId: Long) {
        publishers?.let {
            if (!it.isEmpty) {
                for (i in 0 until it.size()) {
                    val publisher = it[i.toInt()]
                    var userId = publisher.mUserId
                    onMemberJoined(roomId.toString(), userId)
                    val option: BMXRTCSignalService.BMXRoomSubJoinOptions = BMXRTCSignalService.BMXRoomSubJoinOptions(publisher.streams, userId)
                    option.mRoomId = roomId
                    option.mRoomPin = mPin
                    mService.subJoinRoom(mBMXRTCSession, option)
                }
            }
        }
    }

    fun openSubscribe(sdp: SessionDescription?, room: BMXRTCRoom) {
        val bmxSdp = BMXRoomSDPInfo()
        bmxSdp.sdp = sdp?.description
        bmxSdp.type = BMXRoomSDPType.Answer
        mService.subStart(mBMXRTCSession, room, bmxSdp)
    }

    //自己加入房间监听回调
    private fun onJoinRoom(code: Int, msg: String?, roomId: String?) {
        Thread {
            mEngineListeners.forEach {
                it.onJoinRoom(msg, roomId!!.toLong(), if(code == 0) BMXErrorCode.NoError else BMXErrorCode.NotFound)
            }
        }.start()
    }

    //自己发布成功监听回调
    fun onLocalPublish(stream: BMXStream?, info: String?, code: Int) {
        Thread {
            mEngineListeners.forEach {
                it.onLocalPublish(stream, info, if(code == 0) BMXErrorCode.NoError else BMXErrorCode.NotFound)
            }
        }.start()
    }

    //房间存在 开启发布
    private fun startPubJoin(roomId: Long, session: BMXRTCSession?){
        //room创建成功之后调用发布
        mRoomId = roomId.toString()
        session?.let {
            val option: BMXRTCSignalService.BMXPubRoomJoinOptions = BMXRTCSignalService.BMXPubRoomJoinOptions(mUserId?.toLong()
                    ?: 0, roomId)
            option.mRoomPin = mPin
            mService.pubJoinRoom(it, option)
        }
    }

    //别人加入房间监听回调
    private fun onMemberJoined(roomId: String, uid: Long) {
        Thread {
            mEngineListeners.forEach {
                it.onMemberJoined(roomId.toLong(), uid)
            }
        }.start()
    }

    //别人加入房间监听回调
    private fun onRemotePublish(stream: BMXStream?, info: String?, code: Int){
        Thread {
            mEngineListeners.forEach {
                it.onRemotePublish(stream, info, if(code == 0) BMXErrorCode.NoError else BMXErrorCode.NotFound)
            }
        }.start()
    }

    //别人发布成功监听回调
    private fun onSubscribe(stream: BMXStream?, info: String?, code: Int){
        Thread {
            mEngineListeners.forEach {
                it.onSubscribe(stream, info, if(code == 0) BMXErrorCode.NoError else BMXErrorCode.NotFound)
            }
        }.start()
    }

    private fun buildBMXStreamInfo(userId: String, roomId: String, hasVideo: Boolean, hasAudio: Boolean, hasData: Boolean = false, isMuteVideo: Boolean = false, isMuteAudio: Boolean = false): BMXStream {
        val streamInfo = BMXStream()
        streamInfo.mUserId = userId.toLong()
        streamInfo.mStreamId = roomId
        streamInfo.mEnableVideo = hasVideo
        streamInfo.mEnableAudio = hasAudio
        streamInfo.mEnableData = hasData
        streamInfo.mMuteVideo = isMuteVideo
        streamInfo.mMuteAudio = isMuteAudio
        return streamInfo
    }
}
