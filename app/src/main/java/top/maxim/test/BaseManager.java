
package top.maxim.test;

import android.app.Application;
import android.text.TextUtils;

import java.io.File;

import im.floo.floolib.BMXClient;
import im.floo.floolib.BMXClientType;
import im.floo.floolib.BMXLogLevel;
import im.floo.floolib.BMXPushEnvironmentType;
import im.floo.floolib.BMXSDKConfig;

public class BaseManager {

    protected static BMXClient bmxClient;

    static {
        System.loadLibrary("floo");
    }

    /**
     * 配置环境
     */
    public static void initBMXSDK(Application application) {
        String appPath = application.getFilesDir().getPath();
        File dataPath = new File(appPath + "/data_dir");
        File cachePath = new File(appPath + "/cache_dir");
        dataPath.mkdirs();
        cachePath.mkdirs();

        String pushId = "";
        BMXSDKConfig conf = new BMXSDKConfig(BMXClientType.Android, "1", dataPath.getAbsolutePath(),
                cachePath.getAbsolutePath(), TextUtils.isEmpty(pushId) ? "MaxIM" : pushId);
        conf.setAppID("welovemaxim");
        conf.setConsoleOutput(true);
        conf.setLoadAllServerConversations(true);
        conf.setLogLevel(BMXLogLevel.Debug);
        conf.setDeviceUuid("1111");
        conf.setEnvironmentType(BMXPushEnvironmentType.Production);
        bmxClient = BMXClient.create(conf);
        initRTC(application);
    }

    private static void initRTC(Application application){
        RTCManager.getInstance().init(application);
    }
}
