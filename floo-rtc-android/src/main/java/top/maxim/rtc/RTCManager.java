
package top.maxim.rtc;

import android.app.Application;

import im.floo.floolib.BMXClient;
import im.floo.floolib.BMXRTCEngine;
import im.floo.floolib.BMXRTCService;
import im.floo.floolib.BMXRTCServiceListener;
import top.maxim.rtc.engine.MaxEngine;

/**
 * Description : RTC Created by Mango on 2018/12/2.
 */
public class RTCManager{

    private static final String TAG = RTCManager.class.getSimpleName();

    private static RTCManager sInstance;

    private BMXRTCService mService;

    public static RTCManager getInstance() {
        if (sInstance == null){
            sInstance = new RTCManager();
        }
        return sInstance;
    }

    private RTCManager() {
    }

    public void init(Application application, BMXClient bmxClient){
        MaxEngine.Companion.init(application);
        mService = bmxClient.getRTCManager();
        mService.setupRTCEngine(new MaxEngine(bmxClient));

//        UCloudEngine.init(application,"urtc-tcz01qrv","5b289a266a6751cc2a325c638b5cfbc8");
//        mService.setupRTCEngine(new UCloudEngine());
    }

    public BMXRTCEngine getRTCEngine(){
        return mService.getRTCEngine();
    }

    /**
     * 添加监听者
     **/
    public void addRTCServiceListener(BMXRTCServiceListener listener) {
        mService.addRTCServiceListener(listener);
    }

    /**
     * 移除监听者
     **/
    public void removeRTCServiceListener(BMXRTCServiceListener listener) {
        mService.removeRTCServiceListener(listener);
    }
}
