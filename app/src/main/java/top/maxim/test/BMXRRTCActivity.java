package top.maxim.test;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import im.floo.floolib.BMXErrorCode;
import im.floo.floolib.BMXRTCEngine;
import im.floo.floolib.BMXRTCEngineListener;
import im.floo.floolib.BMXRoomAuth;
import im.floo.floolib.BMXStream;
import im.floo.floolib.BMXVideoCanvas;
import im.floo.floolib.BMXVideoMediaType;
import top.maxim.rtc.view.RTCRenderView;

public class BMXRRTCActivity extends AppCompatActivity {

    public static void openVideoCall(Context context) {
        context.startActivity(new Intent(context, BMXRRTCActivity.class));
    }

    private RelativeLayout mContainer;

    private RTCRenderView mLocalView;
    private RTCRenderView mRemoteView;

    private BMXRTCEngine mEngine;

    private BMXRTCEngineListener mListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rtc);
        mLocalView = findViewById(R.id.rtc_local);
        mContainer = findViewById(R.id.rtc_container);
        mEngine = RTCManager.getInstance().getRTCEngine();
        mEngine.addRTCEngineListener(mListener = new BMXRTCEngineListener() {

            @Override
            public void onJoinRoom(String info, String roomId, BMXErrorCode error) {
                super.onJoinRoom(info, roomId, error);
                if (error != null && error.swigValue() == 0) {
                    mEngine.publish(BMXVideoMediaType.Camera, true, true);
                    Log.e("aaaaaaa", "加入房间成功 开启发布本地流, roomId= " + roomId + "msg = " + info);
                } else {
                    Log.e("aaaaaaa", "加入房间失败 roomId= " + roomId + "msg = " + info);
                }
            }

            @Override
            public void onLeaveRoom(String info, String roomId, BMXErrorCode error, String reason) {
                super.onLeaveRoom(info, roomId, error, reason);
                if (error != null && error.swigValue() == 0) {
                    Log.e("aaaaaaa", "离开房间成功 roomId= " + roomId + "msg = " + reason);
                } else {
                    Log.e("aaaaaaa", "离开房间失败 roomId= " + roomId + "msg = " + reason);
                }
            }

            @Override
            public void onReJoinRoom(String info, String roomId, BMXErrorCode error) {
                super.onReJoinRoom(info, roomId, error);
            }

            @Override
            public void onMemberJoined(String roomId, long usedId) {
                super.onMemberJoined(roomId, usedId);
                Log.e("aaaaaaa", "远端用户加入 uid= " + usedId);
            }

            @Override
            public void onMemberExited(String roomId, long usedId, String reason) {
                super.onMemberExited(roomId, usedId, reason);
                Log.e("aaaaaaa", "远端用户离开 uid= " + usedId);
                mContainer.removeAllViews();
            }


            @Override
            public void onLocalPublish(BMXStream stream, String info, BMXErrorCode error) {
                super.onLocalPublish(stream, info, error);
                if (error != null && error.swigValue() == 0) {
                    BMXVideoCanvas canvas = new BMXVideoCanvas();
                    canvas.setMView(mLocalView.getObtainView());
                    canvas.setMStream(stream);
                    mEngine.startPreview(canvas);
                    Log.e("aaaaaaa", "发布本地流成功 开启预览 msg = " + info);
                } else {
                    Log.e("aaaaaaa", "发布本地流失败 msg = " + info);
                }
            }

            @Override
            public void onLocalUnPublish(BMXStream stream, String info, BMXErrorCode error) {
                super.onLocalUnPublish(stream, info, error);
                if (error != null && error.swigValue() == 0) {
                    Log.e("aaaaaaa", "停止发布本地流成功 msg = " + info);
                } else {
                    Log.e("aaaaaaa", "停止发布本地流失败 msg = " + info);
                }
            }

            @Override
            public void onRemotePublish(BMXStream stream, String info, BMXErrorCode error) {
                super.onRemotePublish(stream, info, error);
                mEngine.subscribe(stream);
                Log.e("aaaaaaa", "远端发布流 开启订阅");
            }

            @Override
            public void onRemoteUnPublish(BMXStream stream, String info, BMXErrorCode error) {
                super.onRemoteUnPublish(stream, info, error);
                Log.e("aaaaaaa", "远端取消发布流");
                BMXVideoCanvas canvas = new BMXVideoCanvas();
                canvas.setMView(mRemoteView.getObtainView());
                mEngine.stopRemoteView(canvas);
                mEngine.unSubscribe(stream);
            }

            @Override
            public void onSubscribe(BMXStream stream, String info, BMXErrorCode error) {
                super.onSubscribe(stream, info, error);
                if (error != null && error.swigValue() == 0) {
                    mRemoteView = new RTCRenderView(BMXRRTCActivity.this);
                    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(360, 360);
                    layoutParams.topMargin = 20;
                    mContainer.addView(mRemoteView, layoutParams);
                    mRemoteView.init();
                    BMXVideoCanvas canvas = new BMXVideoCanvas();
                    canvas.setMView(mRemoteView.getObtainView());
                    canvas.setMUserId(stream.getMUserId());
                    canvas.setMStream(stream);
                    mEngine.startRemoteView(canvas);
                    Log.e("aaaaaaa", "订阅远端流成功, 开启预览 msg = " + stream.getMUserId());
                } else {
                    Log.e("aaaaaaa", "订阅远端流失败 msg = " + info);
                }
            }

            @Override
            public void onUnSubscribe(BMXStream stream, String info, BMXErrorCode error) {
                super.onUnSubscribe(stream, info, error);
                if (error != null && error.swigValue() == 0) {
                    Log.e("aaaaaaa", "取消订阅远端流成功, 开启预览 msg = " + info);
                } else {
                    Log.e("aaaaaaa", "取消订阅远端流失败 msg = " + info);
                }
            }

            @Override
            public void onLocalAudioLevel(int volume) {
                super.onLocalAudioLevel(volume);
            }

            @Override
            public void onRemoteAudioLevel(long userId, int volume) {
                super.onRemoteAudioLevel(userId, volume);
            }

            @Override
            public void onKickoff(String info, BMXErrorCode error) {
                super.onKickoff(info, error);
            }

            @Override
            public void onWarning(String info, BMXErrorCode error) {
                super.onWarning(info, error);
            }

            @Override
            public void onError(String info, BMXErrorCode error) {
                super.onError(info, error);
            }

            @Override
            public void onNetworkQuality(BMXStream stream, String info, BMXErrorCode error) {
                super.onNetworkQuality(stream, info, error);
            }

        });
        startCamera();
    }

    //开启摄像头
    private void startCamera(){
        //初始化渲染源
        mLocalView.init();
        BMXRoomAuth auth = new BMXRoomAuth();
        auth.setMUserId(111);
        auth.setMRoomId("1234");
        mEngine.joinRoom(auth);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocalView.release();
//        mLocalView = null;
        mEngine.leaveRoom();
        mEngine.removeRTCEngineListener(mListener);
    }

    @Override
    public void finish() {
        super.finish();
    }
}
