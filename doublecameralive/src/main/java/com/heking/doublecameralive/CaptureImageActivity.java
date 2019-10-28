package com.heking.doublecameralive;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.heking.android.zcamera.CameraEventCallback;
import com.heking.android.zcamera.camera.CameraManager;
import com.heking.android.zcamera.camera.CaptureCallback;
import com.zhoug.android.common.utils.AppUtils;
import com.zhoug.android.common.utils.BitmapUtils;
import com.zhoug.android.common.utils.FileUtils;
import com.zhoug.android.common.utils.IOUtils;

import java.io.File;

/**
 * 调用前后摄像头拍照(分开拍再合成)
 */
public class CaptureImageActivity extends AppCompatActivity {
    private static final String TAG = ">>>CaptureImageActivity";

    private SurfaceView surfaceView;
    private TextView tvCapture;
    private ImageView ivCancel;
    private ImageView ivOk;
    private ImageView ivResult;
    private ImageView ivCache;
    private View uiCaptureFinish;
    private View uiCapture;

    private CameraManager cameraManager;
    private String path1;
    private String path2;
    private boolean two=false;
    private Point screenPoint;

    private String resultPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.live_activity_capture_image);


        findViews();
        initCamera();
    }

    private void findViews(){
        surfaceView=findViewById(R.id.surfaceView);
        tvCapture=findViewById(R.id.tv_capture);
        ivCancel=findViewById(R.id.iv_cancel);
        ivOk=findViewById(R.id.iv_ok);
        ivResult=findViewById(R.id.iv_image);
        ivCache=findViewById(R.id.iv_cache);
        uiCaptureFinish=findViewById(R.id.ui_capture_finish);
        uiCapture=findViewById(R.id.ui_group);

        addListener();
    }

    private void initCamera(){
        cameraManager=new CameraManager();
        cameraManager.setSurfaceView(surfaceView);
        cameraManager.setMirror(true);
        cameraManager.setCaptureCallback((code,path)->{
            if(code==CaptureCallback.SUCCESS){
                if(!two){
                    path1=path;
                    Bitmap  bitmap=BitmapUtils.decodeFile(path1, getScreenSize().x,getScreenSize().y , Bitmap.Config.RGB_565);
                    ivCache.setImageBitmap(bitmap);
                    two=true;
                    cameraManager.changeCamera();
                }else{
                    path2=path;
                    Bitmap  bitmap1=BitmapUtils.decodeFile(path1, getScreenSize().x,getScreenSize().y , Bitmap.Config.RGB_565);
                    Bitmap  bitmap2=BitmapUtils.decodeFile(path2, getScreenSize().x,getScreenSize().y , Bitmap.Config.RGB_565);
                    Bitmap bitmap = Utils.drawBitmap(bitmap1, bitmap2);
                    ivResult.setImageBitmap(bitmap);
                    keep(bitmap);
                    bitmap1.recycle();
                    bitmap2.recycle();
                    two=false;
                    cameraManager.changeCamera();
                    cameraManager.stopPreview();
                    ivCache.setImageBitmap(null);
                    ivCache.setVisibility(View.GONE);
                    uiCapture.setVisibility(View.GONE);
                    uiCaptureFinish.setVisibility(View.VISIBLE);

                }
            }

        });

        cameraManager.setCameraEventCallback(new CameraEventCallback() {
            @Override
            public void onOpenCameraError(int errorCode, String msg) {
                 Toast.makeText(CaptureImageActivity.this, "打开相机失败", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onOpenCameraSuccess(Camera camera) {

            }
        });
        cameraManager.init();

    }

    /**
     * 保存
     * @param bitmap
     */
    private void keep(Bitmap bitmap){
        if(resultPath!=null){
            FileUtils.deleteFile(new File(resultPath));
        }
        File externalFile = FileUtils.getExternalFile(Environment.DIRECTORY_PICTURES, System.currentTimeMillis() + ".jpg");
        resultPath=externalFile.getAbsolutePath();
        IOUtils.keepFile(externalFile.getAbsolutePath(),BitmapUtils.compressQuality(bitmap,100 ));
    }

    private void addListener(){
        tvCapture.setOnClickListener(v->{
            cameraManager.captureImage();
        });

        ivCancel.setOnClickListener(v->{
            ivCache.setVisibility(View.VISIBLE);
            uiCapture.setVisibility(View.VISIBLE);
            uiCaptureFinish.setVisibility(View.GONE);
            cameraManager.startPreview();

        });
        ivOk.setOnClickListener(v->{
            Intent intent=new Intent();
            intent.putExtra("path",resultPath );
            setResult(RESULT_OK,intent );
            finish();
        });

    }

    private Point getScreenSize(){
        if(screenPoint==null){
            screenPoint= AppUtils.getScreenSize(this);
        }
        return screenPoint;
    }

}
