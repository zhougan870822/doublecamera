package com.heking.doublecameralive;


import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.heking.android.zcamera.CameraEventCallback;
import com.heking.android.zcamera.camera.CameraManager;
import com.heking.android.zcamera.camera.CaptureCallback;
import com.heking.doublecameralive.network.HttpClient;
import com.heking.doublecameralive.network.HttpRequest;
import com.zhoug.android.common.utils.AppUtils;
import com.zhoug.android.common.utils.BitmapUtils;
import com.zhoug.android.common.utils.StringUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jp.co.cyberagent.android.gpuimage.GPUImageAddBlendFilter;
import me.lake.librestreaming.core.listener.RESConnectionListener;
import me.lake.librestreaming.filter.hardvideofilter.BaseHardVideoFilter;
import me.lake.librestreaming.filter.hardvideofilter.HardVideoGroupFilter;
import me.lake.librestreaming.ws.StreamAVOption;
import me.lake.librestreaming.ws.StreamLiveCameraView;
import me.lake.librestreaming.ws.filter.hardfilter.GPUImageBeautyFilter;
import me.lake.librestreaming.ws.filter.hardfilter.extra.GPUImageCompatibleFilter;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 直播后摄像头(先前摄像头拍用户头像)
 */
public class LiveActivity extends AppCompatActivity {
    private static final String TAG = ">>>LiveActivity";

    private SurfaceView mSurfaceView;
    private ShadeImageView shadeImageView;
    private ImageView ivFront;

    private StreamLiveCameraView mLiveCameraView;
    private TextView mBtnStart;
    private TextView mBtnCaptureImage;

    private String photoPath;

    private String publishUrl = "rtmp://hk.download.kumiko.cc/live/uid001_b";

    private Camera mCamera;
    private CameraInfo mCameraInfo;
    private Parameters mParameters;

    private int WIDTH_DEF = 720;//1280
    private int HEIGHT_DEF = 1280;//720

    private final int videoBitrate = 300 * 1024 * 8;//码率
    private final int videoFramerate = 30;//帧率

    private Point screenSize;
    private StreamAVOption streamAVOption;

    private CameraManager mCameraManager;

    public static final String KEY_URL = "key_url";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.live_activity_live);
        initData();
        findViews();
    }

    private void initData() {
        Intent intent = getIntent();
        if (null != intent) {
            String url = intent.getStringExtra(KEY_URL);
            if (!TextUtils.isEmpty(url)) {
                publishUrl = url;
            }
        }

        Log.i(TAG, "publishUrl:" + publishUrl);
    }


    private void findViews() {
        mSurfaceView = findViewById(R.id.surfaceView);
        mLiveCameraView = findViewById(R.id.stream_previewView);
        shadeImageView = findViewById(R.id.shadeImageView);
        ivFront = findViewById(R.id.iv_front);

        mBtnStart = findViewById(R.id.btn_start);
        mBtnCaptureImage = findViewById(R.id.btn_capture_image);

        initCameraFront();
    }

    private void initCameraFront() {
        setUiVisibility(true);
        mBtnCaptureImage.setOnClickListener(v -> {
            if (mCameraManager != null) {
                mCameraManager.captureImage();
            }
        });

        mCameraManager = new CameraManager();
        mCameraManager.setCameraFacing(CameraInfo.CAMERA_FACING_FRONT);
        mCameraManager.setSurfaceView(mSurfaceView);
        mCameraManager.setMirror(true);
        mCameraManager.setContinuePreView(false);
        mCameraManager.setCaptureCallback(new CaptureCallback() {
            @Override
            public void onResult(int code, String path) {
                if (code == CaptureCallback.SUCCESS) {
                    photoPath = path;
//                    upload();
                    upload2();
//                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    initLive();
                } else if (code == CaptureCallback.FAILURE) {
                    toast("拍照失败");
                }
            }
        });
        mCameraManager.setCameraEventCallback(new CameraEventCallback() {
            @Override
            public void onOpenCameraError(int errorCode, String msg) {
                Toast.makeText(LiveActivity.this, "开启相机失败", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onOpenCameraSuccess(android.graphics.Camera camera) {

            }
        });
        mCameraManager.init();

    }

    /**
     *     Observable<Response> post(@Path("url") String url,
     *              @PartMap() Map<String, RequestBody> params,
     *              @Part List<MultipartBody.Part> files);
     */
    private void upload2(){
        Log.d(TAG, "upload:photoPath="+photoPath);
        if(TextUtils.isEmpty(photoPath)){
            return;
        }

        String UID ="";
        String FirmID="";
        if(publishUrl!=null){
            // rtsp://hk.download.kumiko.cc/live/uid001_zxczx_b
            String[] sd = publishUrl.split("/");
            String s1= sd[sd.length-1];
            String s2= sd[sd.length-2];
            String uid_firmId="";
            if(!StringUtils.isEmpty(s1)){
                uid_firmId=s1;
            }else {
                uid_firmId=s2;
            }

            String[] split = uid_firmId.split("_");
            if (split.length >= 2) {
                UID=split[0];
                FirmID=split[1];
            }
        }

        Log.d(TAG, "upload:UID="+UID+",FirmID"+FirmID);
        Map<String,Object> params=new HashMap<>();
        params.put("UID", UID);
        params.put("FirmID",FirmID );

        List<String> files=new ArrayList<>();
        files.add(photoPath);

        Call<ResponseBody> post = HttpClient.post(HttpRequest.uploadLivePhoto, params, files);
        post.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if(response!=null && response.isSuccessful()){
                    ResponseBody body = response.body();
                    if(body!=null){
                        try {
                            String result = body.string();
                            //{"state":200,"message":"保存成功","token":null,"data":null}
                            JSONObject jsonObject=new JSONObject(result);
                            int state = jsonObject.getInt("state");
                            if(state==200){
                                Log.i(TAG, "上传头像成功");
                            }else{
                                toast("上传头像失败");
                                Log.e(TAG, result);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                toast("上传头像失败");
                t.printStackTrace();
            }
        });


    }
    /**
     * 上传头像
     */
    private void upload(){
        Log.d(TAG, "upload:photoPath="+photoPath);
        if(TextUtils.isEmpty(photoPath)){
            return;
        }

        String UID ="";
        String FirmID="";
        if(publishUrl!=null){
           // rtsp://hk.download.kumiko.cc/live/uid001_zxczx_b
            String[] sd = publishUrl.split("/");
            String s1= sd[sd.length-1];
            String s2= sd[sd.length-2];
            String uid_firmId="";
            if(!StringUtils.isEmpty(s1)){
                uid_firmId=s1;
            }else {
                uid_firmId=s2;
            }

            String[] split = uid_firmId.split("_");
            if (split.length >= 2) {
                UID=split[0];
                FirmID=split[1];
            }
        }

        Log.d(TAG, "upload:UID="+UID+",FirmID"+FirmID);
        Map<String,String> params=new HashMap<>();
        params.put("UID", UID);
        params.put("FirmID",FirmID );

        List<String> files=new ArrayList<>();
        files.add(photoPath);


        try {
            Class AppClientCls=Class.forName("com.heking.qlymclz.network.CommonRequest");
            //postUploadRtmpPhoto
            Log.d(TAG, "upload:AppClientCls="+AppClientCls);
            Method postUploadRtmpPhoto = AppClientCls.getDeclaredMethod("postUploadRtmpPhoto",Map.class,List.class);
            Log.d(TAG, "upload:postUploadRtmpPhoto="+postUploadRtmpPhoto);

            postUploadRtmpPhoto.invoke(null,params,files);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    private void setUiVisibility(boolean front) {
        if (front) {
            mLiveCameraView.setVisibility(View.GONE);
            mBtnStart.setVisibility(View.GONE);
            ivFront.setVisibility(View.GONE);

            mSurfaceView.setVisibility(View.VISIBLE);
            mBtnCaptureImage.setVisibility(View.VISIBLE);
//            shadeImageView.setVisibility(View.VISIBLE);

        } else {
            mSurfaceView.setVisibility(View.GONE);
            mBtnCaptureImage.setVisibility(View.GONE);
            shadeImageView.setVisibility(View.GONE);

            mLiveCameraView.setVisibility(View.VISIBLE);
            mBtnStart.setVisibility(View.VISIBLE);
            ivFront.setVisibility(View.VISIBLE);

        }

    }


    private void initLive() {
        setUiVisibility(false);
        //参数配置 start
        streamAVOption = new StreamAVOption();
        streamAVOption.streamUrl = publishUrl;
        streamAVOption.cameraIndex = 0;
        streamAVOption.previewWidth = WIDTH_DEF;
        streamAVOption.previewHeight = HEIGHT_DEF;
        streamAVOption.videoWidth = WIDTH_DEF;
        streamAVOption.videoHeight = HEIGHT_DEF;
        StreamAVOption.recordVideoWidth = WIDTH_DEF;
        StreamAVOption.recordVideoHeight = HEIGHT_DEF;
        streamAVOption.videoBitrate = videoBitrate;//
        streamAVOption.videoFramerate = videoFramerate;//帧率
        mLiveCameraView.init(this, streamAVOption);

        //设置滤镜组
        LinkedList<BaseHardVideoFilter> files = new LinkedList<>();
        files.add(new GPUImageCompatibleFilter(new GPUImageBeautyFilter()));
        files.add(new GPUImageCompatibleFilter(new GPUImageAddBlendFilter()));


        Bitmap bitmap = BitmapUtils.decodeFile(photoPath, AppUtils.dipTopx(this, 120), AppUtils.dipTopx(this, 160), Bitmap.Config.RGB_565);
        if (bitmap != null) {
           /* Log.d(TAG, "init:bitmap size:" + bitmap.getWidth() + "," + bitmap.getHeight());
//            int left = streamAVOption.videoWidth -bitmap.getWidth();

            Rect rect = null;
            int left = streamAVOption.videoHeight - bitmap.getWidth();
            int top = streamAVOption.videoHeight - bitmap.getHeight();
            rect = new Rect(left, top, left + bitmap.getWidth(), top + bitmap.getHeight());

            Log.d(TAG, "initLive:" + rect.toString());
            files.add(new WatermarkFilter(bitmap, rect));*/

            if (ivFront != null) {
//                ivFront.setImageBitmap(Bitmap.createBitmap(bitmap));
                ivFront.setImageBitmap(bitmap);
            }
        }

        mLiveCameraView.setHardVideoFilter(new HardVideoGroupFilter(files));

        mLiveCameraView.addStreamStateListener(resConnectionListener);


        mBtnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mLiveCameraView.isStreaming()) {
                    startPublish(true);
                } else {
                    startPublish(false);

                }
            }
        });
    }

    private synchronized void startPublish(boolean start) {
        if (start && !mLiveCameraView.isStreaming()) {
            mLiveCameraView.startStreaming(publishUrl);
            mBtnStart.setText("停止");
        } else if (!start && mLiveCameraView.isStreaming()) {
            mLiveCameraView.stopStreaming();
            mBtnStart.setText("开始");
        }

        if(start){
            mBtnStart.setText("停止");
        }else{
            mBtnStart.setText("开始");
        }
    }


    private RESConnectionListener resConnectionListener = new RESConnectionListener() {
        @Override
        public void onOpenConnectionResult(int i) {
            //0 连接服务器成功，1连接服务器失败
            if (i == 1) {
                toast("服务器连接失败");
                startPublish(false);
            }
            Log.d(TAG, "onOpenConnectionResult:i=" + i);
        }

        @Override
        public void onWriteError(int i) {
            Log.d(TAG, "onWriteError:i=" + i);
            startPublish(false);

        }

        @Override
        public void onCloseConnectionResult(int i) {
            Log.d(TAG, "onCloseConnectionResult:i=" + i);
            startPublish(false);

        }
    };


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.d(TAG, "onConfigurationChanged:横屏");
            initLive();
        } else {
            Log.d(TAG, "onConfigurationChanged:竖屏");

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mLiveCameraView != null) {
                mLiveCameraView.destroy();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }


}
