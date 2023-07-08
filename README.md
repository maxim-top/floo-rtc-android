# 蓝莺IM RTC SDK，安卓本地库

蓝莺IM，是由[美信拓扑](https://www.maximtop.com/)团队研发的新一代即时通讯云服务，SDK设计简单集成方便，服务采用云原生技术和多云架构，私有云也可按月付费。

蓝莺IM RTC SDK，则是在蓝莺IM SDK的基础上，以IM作为信令通道，提供了实时音视频功能，可以帮助用户快速将实时音视频功能集成到App中。目前的版本提供一对一的视频通话和语音通话功能。

[![Scc Count Badge](https://sloc.xyz/github/maxim-top/floo-rtc-android/?category=total&avg-wage=1)](https://github.com/maxim-top/floo-rtc-android/) [![Scc Count Badge](https://sloc.xyz/github/maxim-top/floo-rtc-android/?category=code&avg-wage=1)](https://github.com/maxim-top/floo-rtc-android/)

## 介绍

为最大程度减少用户的开发成本，降低开发难度，我们将复杂的RTC业务逻辑隐藏在RTCManager之下，将信令部分的实现由floo-android提供。

整个SDK分为两大部分：

1. RTCManager

RTCManager负责管理BMXRTCEngine、BMXRTCEngineListener、BMXRTCService和BMXRTCServiceListener。

其中：
BMXRTCEngine用于提供RTC相关业务功能，比如进入房间、退出房间、发布流、订阅流等；
BMXRTCEngineListener用于接收RTC相关业务事件，比如发布流成功、退出房间成功等；
BMXRTCService用于发送RTC基本类型的消息，有发起呼叫、接听呼叫、结束通话；
BMXRTCServiceListener用于接收RTC基本类型的消息。如果您还有其它类型的自定义消息，可以通过floo-android提供的消息机制创建自定义消息，发送和接收。

2. BMXRtcRenderView

BMXRtcRenderView提供了视频画面渲染功能，本地和远程视频画面都可以使用。

## 开发

### 创建用户界面
1. 导入视频画面类依赖
```
import top.maxim.rtc.view.BMXRtcRenderView;
import top.maxim.rtc.view.RTCRenderView;
```

2. 在通话界面创建两个画面的容器布局（本例中为大画面全屏，小画面居右上）
```
    <FrameLayout
        android:id="@+id/video_view_container_large"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="@color/color_black" />

    <FrameLayout
        android:id="@+id/video_view_container_small"
        android:layout_width="120dp"
        android:layout_height="212dp"
        android:background="@color/color_black"
        android:layout_gravity="right"
        android:visibility="gone" />

```

3. 添加本地画面到小画面容器布局
```
        ViewGroup smallViewGroup = mVideoContainer.findViewById(R.id.video_view_container_small);
        //呼叫过程中，对方画面为空，则将本地画面全屏
        if (mRemoteView == null){
            ViewGroup.LayoutParams layoutParams = smallViewGroup.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            smallViewGroup.setLayoutParams(layoutParams);
        }
        smallViewGroup.setVisibility(View.VISIBLE);
        mLocalView = new RTCRenderView(this);
        mLocalView.init();
        mLocalView.setScalingType(BMXRtcRenderView.ScalingType.SCALE_ASPECT_FILL);
        mLocalView.getSurfaceView().setZOrderMediaOverlay(true);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
        smallViewGroup.addView(mLocalView, layoutParams);
```
4. 添加对方画面到大画面容器布局
```
        //将原本全屏的本地画面还原为小尺寸
        if (mLocalView != null){
            ViewGroup smallViewGroup = mVideoContainer.findViewById(R.id.video_view_container_small);
            ViewGroup.LayoutParams layoutParams = smallViewGroup.getLayoutParams();
            layoutParams.width = getPixelsFromDp(120);
            layoutParams.height = getPixelsFromDp(212);
            smallViewGroup.setLayoutParams(layoutParams);
            mLocalView.setScalingType(BMXRtcRenderView.ScalingType.SCALE_ASPECT_FILL);
        }

        ViewGroup largeViewGroup = mVideoContainer.findViewById(R.id.video_view_container_large);
        largeViewGroup.setVisibility(View.VISIBLE);
        mRemoteView = new RTCRenderView(this);
        mRemoteView.init();
        mRemoteView.setScalingType(BMXRtcRenderView.ScalingType.SCALE_ASPECT_FILL);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        largeViewGroup.addView(mRemoteView, layoutParams);

```

### 音视频通话业务逻辑
1. 导入RTCManager

```
import top.maxim.rtc.RTCManager;
```

2. 添加事件监听

```
mEngine = RTCManager.getInstance().getRTCEngine();
mEngine.addRTCEngineListener(mListener = new BMXRTCEngineListener() {

    @Override
    public void onJoinRoom(String info, long roomId, BMXErrorCode error) {
        super.onJoinRoom(info, roomId, error);
        mRoomId = roomId;
        if (BaseManager.bmxFinish(error)) {
            mEngine.publish(BMXVideoMediaType.Camera, mHasVideo, true);
            Log.e(TAG, "加入房间成功 开启发布本地流, roomId= " + roomId + "msg = " + info);
        } else {
            Log.e(TAG, "加入房间失败 roomId= " + roomId + "msg = " + info);
        }
    }

    @Override
    public void onLeaveRoom(String info, long roomId, BMXErrorCode error, String reason) {
        super.onLeaveRoom(info, roomId, error, reason);
        if (BaseManager.bmxFinish(error)) {
            Log.e(TAG, "离开房间成功 roomId= " + roomId + "msg = " + reason);
        }else{
            Log.e(TAG, "离开房间失败 roomId= " + roomId + "msg = " + reason);
        }
    }

    @Override
    public void onMemberJoined(long roomId, long usedId) {
        super.onMemberJoined(roomId, usedId);
        Log.e(TAG, "远端用户加入 uid= " + usedId);
    }

    @Override
    public void onMemberExited(long roomId, long usedId, String reason) {
        super.onMemberExited(roomId, usedId, reason);
        Log.e(TAG, "远端用户离开 uid= " + usedId);
        //回收界面
        onRemoteLeave();
    }

    @Override
    public void onLocalPublish(BMXStream stream, String info, BMXErrorCode error) {
        super.onLocalPublish(stream, info, error);
        if (BaseManager.bmxFinish(error)) {
            //打开本地视频画面，发送呼叫消息
            onUserJoin(stream);
            Log.e(TAG, "发布本地流成功 开启预览 msg = " + info);
        }else{
            Log.e(TAG, "发布本地流失败 msg = " + info);
        }
    }

    @Override
    public void onLocalUnPublish(BMXStream stream, String info, BMXErrorCode error) {
        super.onLocalUnPublish(stream, info, error);
        if (BaseManager.bmxFinish(error)) {
            Log.e(TAG, "停止发布本地流成功 msg = " + info);
        }else{
            Log.e(TAG, "停止发布本地流失败 msg = " + info);
        }
    }

    @Override
    public void onRemotePublish(BMXStream stream, String info, BMXErrorCode error) {
        super.onRemotePublish(stream, info, error);
        if (BaseManager.bmxFinish(error)) {
            mEngine.subscribe(stream);
            Log.e(TAG, "远端发布流 开启订阅");
        }else{
            Log.e(TAG, "远端发布流失败 msg = " + info);
        }
    }

    @Override
    public void onRemoteUnPublish(BMXStream stream, String info, BMXErrorCode error) {
        super.onRemoteUnPublish(stream, info, error);
        if (BaseManager.bmxFinish(error)) {
            Log.e(TAG, "远端取消发布流");
            BMXVideoCanvas canvas = new BMXVideoCanvas();
            canvas.setMStream(stream);
            mEngine.stopRemoteView(canvas);
            //停止订阅
            mEngine.unSubscribe(stream);
            //回收界面
            onRemoteLeave();
        }else{
            Log.e(TAG, "远端取消发布流失败 msg = " + info);
        }
    }

    @Override
    public void onSubscribe(BMXStream stream, String info, BMXErrorCode error) {
        super.onSubscribe(stream, info, error);
        if (BaseManager.bmxFinish(error)) {
            //展示对方视频画面
            onRemoteJoin(stream);
            mPickupTimestamp = getTimeStamp();
            Log.e(TAG, "订阅远端流成功 msg = " + info);
        } else {
            Log.e(TAG, "订阅远端流失败 msg = " + info);
        }
    }

    @Override
    public void onUnSubscribe(BMXStream stream, String info, BMXErrorCode error) {
        super.onUnSubscribe(stream, info, error);
        if (BaseManager.bmxFinish(error)) {
            Log.e(TAG, "取消订阅远端流成功, 开启预览 msg = " + info);
        } else {
            Log.e(TAG, "取消订阅远端流失败 msg = " + info);
        }
    }
});
```

3. 加入房间

```
    //设置视频分辨率
    BMXVideoConfig config = new BMXVideoConfig();
    config.setProfile(EngineConfig.VIDEO_PROFILE);
    mEngine.setVideoProfile(config);

    //设置用户ID、pin密码和房间ID
    BMXRoomAuth auth = new BMXRoomAuth();
    auth.setMUserId(mUserId);
    auth.setMRoomId(mRoomId);//主叫方无须设置roomId，房间创建成功事件会返回系统分配的roomId；被叫方需要设置与主叫方一样的roomId
    auth.setMToken(mPin);//房间pin密码，建议随机生成高强度密码
    mEngine.joinRoom(auth);
```

4. 挂断通话

```
    public void onCallHangup(View view){
        //发送挂断消息
        sendRTCHangupMessage();
        leaveRoom();
        finish();
    }
```

5. 回收界面

```
    private void onRemoteLeave() {
        removeRemoteView();
        mHandler.removeAll();
        finish();
    }
```

6. 打开本地画面，发送呼叫消息

```
    private void onUserJoin(BMXStream info){
        if(info == null){
            return;
        }
        if (mHasVideo) {
            runOnUiThread(() -> {
                addLocalView();
                BMXVideoCanvas canvas = new BMXVideoCanvas();
                canvas.setMView(mLocalView.getObtainView());
                canvas.setMStream(info);
                mEngine.startPreview(canvas);
            });
        } else {

        }
        if (mIsInitiator) {
            //用户加入放入房间 发送给对方信息
            sendRTCCallMessage();
        }
    }
```

7. 打开对方画面

```
    private void onRemoteJoin(BMXStream info) {
        if(info == null){
            return;
        }
        runOnUiThread(() -> {
            if (mHasVideo) {
                addRemoteView();
                BMXVideoCanvas canvas = new BMXVideoCanvas();
                canvas.setMView(mRemoteView.getObtainView());
                canvas.setMUserId(info.getMUserId());
                canvas.setMStream(info);
                mEngine.startRemoteView(canvas);
            }
        });
    }
```

8. 通话结束时回收资源

```
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mEngine != null) {
            mEngine.removeRTCEngineListener(mListener);
            mListener = null;
        }
        RTCManager.getInstance().removeRTCServiceListener(mRTCListener);
        mRTCListener = null;
    }
```

### 使用rtcService实现音视频通话信令

1. 添加事件监听

private BMXRTCServiceListener mRTCListener = new BMXRTCServiceListener(){

    public void onRTCCallMessageReceive(BMXMessage msg) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //暂停以处理稍后收到的对应通话的挂断消息（mHungupCalls），
                    // 这样可以避免弹出已结束的通话
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                String callId = msg.config().getRTCCallId();
                if (mHungupCalls.contains(callId)){
                    mHungupCalls.remove(callId);
                    ackMessage(msg);
                    return;
                }
                long roomId = msg.config().getRTCRoomId();
                long chatId = msg.config().getRTCInitiator();
                long myId = SharePreferenceUtils.getInstance().getUserId();
                if (myId == chatId){
                    return;
                }
                //如果已在通话中，则发送忙线消息给对方
                if (RTCManager.getInstance().getRTCEngine().isOnCall){
                    replyBusy(callId, myId, chatId);
                    return;
                }
                String pin = msg.config().getRTCPin();
                if(mActivityRef != null && mActivityRef.get() != null){
                    Context context = mActivityRef.get();
                    if (msg.type() == BMXMessage.MessageType.Single) {
                        //打开通话界面（呼入中）
                        SingleVideoCallActivity.openVideoCall(context, chatId, roomId, callId,
                                false, msg.config().getRTCCallType(), pin, msg.msgId());
                    }
                }
            }
        }, "onRTCCallMessageReceive").start();
    }

    public void onRTCPickupMessageReceive(BMXMessage msg) {
        if (msg.config().getRTCCallId().equals(mCallId) && msg.fromId() == mUserId){
            leaveRoom();
            ackMessage(msg);
        }
    }

    public void onRTCHangupMessageReceive(BMXMessage msg) {
        long otherId = mEngine.otherId;
        if (msg.config().getRTCCallId().equals(mCallId) &&
                (msg.fromId()==otherId
                || msg.content().equals("busy")
                || msg.content().equals("rejected")
                || msg.content().equals("canceled")
                || msg.content().equals("timeout")
                || !mEngine.isOnCall)){
            leaveRoom();
            ackMessage(msg);
        }
    }

};
RTCManager.getInstance().addRTCServiceListener(mRTCListener);


2. 发送呼叫消息

```
public String sendRTCCallMessage(BMXMessageConfig.RTCCallType type, long roomId, long from, long to,
                                    String pin) {
    BMXMessageConfig con = BMXMessageConfig.createMessageConfig(false);
    con.setRTCCallInfo(type, roomId, from, BMXMessageConfig.RTCRoomType.Broadcast, pin);
    con.setIOSConfig("{\"mutable_content\":true,\"loc-key\":\"call_in\"}");
    BMXMessage msg = BMXMessage.createRTCMessage(from, to, BMXMessage.MessageType.Single, to, "");
    msg.setConfig(con);
    msg.setExtension("{\"rtc\":\"call\"}");
    handlerMessage(msg);
    return con.getRTCCallId();
}
```

3. 发送接听消息

```
public void sendRTCPickupMessage(long from, long to, String callId) {
    BMXMessageConfig con = BMXMessageConfig.createMessageConfig(false);
    con.setRTCPickupInfo(callId);
    BMXMessage msg = BMXMessage.createRTCMessage(from, to, BMXMessage.MessageType.Single, to, "");
    msg.setConfig(con);
    handlerMessage(msg);
}
```

4. 发送挂断消息

```
public void sendRTCHangupMessage(long from, long to, String callId, String content, String iosConfig) {
    BMXMessageConfig con = BMXMessageConfig.createMessageConfig(false);
    con.setRTCHangupInfo(callId);
    BMXMessage msg = BMXMessage.createRTCMessage(from, to, BMXMessage.MessageType.Single, to, content);
    msg.setConfig(con);
    handlerMessage(msg);
}
```

快速集成文档参考[蓝莺快速集成指南android版](https://docs.lanyingim.com/quick-start/floo-android-quick-start.html)，
详细文档可参考[floo-android reference](https://docs.lanyingim.com/reference/floo-android.html)

祝玩得开心😊
