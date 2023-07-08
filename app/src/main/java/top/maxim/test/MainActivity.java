package top.maxim.test;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import im.floo.BMXCallBack;
import im.floo.floolib.BMXErrorCode;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.tv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BaseManager.bmxClient.getUserManager().signInByName("8100", "123456", new BMXCallBack() {
                    @Override
                    public void onResult(BMXErrorCode bmxErrorCode) {
                        BMXRRTCActivity.openVideoCall(MainActivity.this);
                    }
                });
            }
        });
    }
}