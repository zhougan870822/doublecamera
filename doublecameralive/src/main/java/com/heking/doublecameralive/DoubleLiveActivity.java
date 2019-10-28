package com.heking.doublecameralive;


import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.zhoug.android.common.utils.AppUtils;

import java.util.LinkedList;

import jp.co.cyberagent.android.gpuimage.GPUImageAddBlendFilter;
import me.lake.librestreaming.core.listener.RESConnectionListener;
import me.lake.librestreaming.filter.hardvideofilter.BaseHardVideoFilter;
import me.lake.librestreaming.filter.hardvideofilter.HardVideoGroupFilter;
import me.lake.librestreaming.ws.StreamAVOption;
import me.lake.librestreaming.ws.StreamLiveCameraView;
import me.lake.librestreaming.ws.filter.hardfilter.GPUImageBeautyFilter;
import me.lake.librestreaming.ws.filter.hardfilter.extra.GPUImageCompatibleFilter;

/**
 * 前后摄像头同时直播
 */
public class DoubleLiveActivity extends AppCompatActivity {
    private static final String TAG = ">>>DoubleLive";

    private StreamLiveCameraView mLiveCameraViewBack;

    private LiveFragment mFrontLiveFragment;

    private Button btnStart;

    private String publishUrlFront = "rtmp://hk.download.kumiko.cc/live/uid001__fid001_f";
    private String publishUrlBack = "rtmp://hk.download.kumiko.cc/live/uid001__fid001_b";


    private int WIDTH_DEF = 1280;
    private int HEIGHT_DEF = 720;
    private final int videoBitrate=300*1024*8;//码率
    private final int videoFramerate=30;//帧率

    private Point screenSize;
    private StreamAVOption streamAVOptionBack;

    private volatile boolean isStart=false;
    public static final String KEY_URL_FRONT="key_url_front";
    public static final String KEY_URL_BACK="key_url_back";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.live_activity_double_live);
        initData();
        findViews();

    }
    private void initData(){
        Intent intent = getIntent();
        if(null!=intent){
            String urlFront=intent.getStringExtra(KEY_URL_FRONT);
            String urlBack=intent.getStringExtra(KEY_URL_BACK);
            if(!TextUtils.isEmpty(urlFront)){
                publishUrlFront=urlFront;
            }
            if(!TextUtils.isEmpty(urlBack)){
                publishUrlBack=urlBack;
            }
        }

        Log.i(TAG, "publishUrlFront:"+publishUrlFront);
        Log.i(TAG, "publishUrlBack:"+publishUrlBack);
    }

    private void findViews() {
        mLiveCameraViewBack = findViewById(R.id.stream_previewView_back);
        btnStart = findViewById(R.id.btn_start);


        if (screenSize == null) {
            screenSize = AppUtils.getScreenSize(this);
        }

        init();
        initFrontLive();

        btnStart.setOnClickListener(v->{
            if(isStart){
                stop();
            }else{
                start();
            }


        });
    }


    private synchronized void start(){
        try {
            if(!isStart){
                mLiveCameraViewBack.startStreaming(publishUrlBack);
                mFrontLiveFragment.startPublish();
                btnStart.setText("停止");
                isStart=true;

            }
        } catch (Exception e) {
            e.printStackTrace();
             Toast.makeText(this, "不支持同时开启前后摄像头", Toast.LENGTH_SHORT).show();
             finish();
        }
    }

    private synchronized void stop(){
        if(isStart){
            mLiveCameraViewBack.stopStreaming();
            mFrontLiveFragment.stopPublish();
            btnStart.setText("开始");
            isStart=false;

        }
    }


    private void init() {

        //参数配置 start
        streamAVOptionBack = new StreamAVOption();
        streamAVOptionBack.streamUrl = publishUrlBack;
        streamAVOptionBack.cameraIndex = 0;
        streamAVOptionBack.previewWidth = WIDTH_DEF;
        streamAVOptionBack.previewHeight = HEIGHT_DEF;
        streamAVOptionBack.videoWidth = WIDTH_DEF;
        streamAVOptionBack.videoHeight = HEIGHT_DEF;
        streamAVOptionBack.videoBitrate = videoBitrate;
        streamAVOptionBack.videoFramerate = videoFramerate;
        mLiveCameraViewBack.init(this, streamAVOptionBack);

        //设置滤镜组
        LinkedList<BaseHardVideoFilter> files = new LinkedList<>();
        files.add(new GPUImageCompatibleFilter(new GPUImageBeautyFilter()));
        files.add(new GPUImageCompatibleFilter(new GPUImageAddBlendFilter()));

        mLiveCameraViewBack.setHardVideoFilter(new HardVideoGroupFilter(files));
        mLiveCameraViewBack.addStreamStateListener(resConnectionListener);

    }

    private RESConnectionListener resConnectionListener = new RESConnectionListener() {
        @Override
        public void onOpenConnectionResult(int i) {
            //0 连接服务器成功，1连接服务器失败
            if(i!=0){
                stop();
            }

            Log.d(TAG, "onOpenConnectionResult:i=" + i);
        }

        @Override
        public void onWriteError(int i) {
            Log.d(TAG, "onWriteError:i=" + i);
        }

        @Override
        public void onCloseConnectionResult(int i) {
            Log.d(TAG, "onCloseConnectionResult:i=" + i);
        }
    };


    private void initFrontLive(){
        FragmentManager manager= getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = manager.beginTransaction();
        Fragment fragment = manager.findFragmentByTag("mFrontLiveFragment");
        if(fragment!=null){
            mFrontLiveFragment= (LiveFragment) fragment;
        }else{
            mFrontLiveFragment =LiveFragment.newInstance(CameraInfo.CAMERA_FACING_FRONT,publishUrlFront);
        }
        fragmentTransaction.add(R.id.container_front, mFrontLiveFragment,"mFrontLiveFragment");
        fragmentTransaction.commit();


    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);


    }

    @Override
    protected void onDestroy() {
        try {
            if(mLiveCameraViewBack!=null){
                mLiveCameraViewBack.destroy();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

}
