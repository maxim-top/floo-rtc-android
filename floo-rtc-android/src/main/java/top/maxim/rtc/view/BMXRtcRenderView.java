package top.maxim.rtc.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.webrtc.RendererCommon;

/**
 * Description : RenderView
 * Created by mango on 5/16/21.
 */
public abstract class BMXRtcRenderView extends FrameLayout {

    Context mContext;

    View obtainView;

    public static enum ScalingType {
        SCALE_ASPECT_FIT,
        SCALE_ASPECT_FILL,
        SCALE_ASPECT_BALANCED;

        private ScalingType() {
        }
    }

    public BMXRtcRenderView(@NonNull Context context) {
        this(context, null);
    }

    public BMXRtcRenderView(@NonNull Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BMXRtcRenderView(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        obtainView = obtainView(context, attrs, defStyleAttr);
        if (obtainView != null) {
            addView(obtainView, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }

    abstract View obtainView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr);

    public View getObtainView(){
        return obtainView;
    }

    public void init(){

    }

    public SurfaceView getSurfaceView(){
        return null;
    }

    public void release(){

    }

    public void setScalingType(ScalingType scalingType){
        CoreRTCRenterView view = (CoreRTCRenterView) getObtainView();
        view.setScalingType(RendererCommon.ScalingType.valueOf(scalingType.name()));
    }

    public void setEnableHardwareScaler(boolean enable){

    }

    public void setMirror(boolean mirror){

    }

}
