package com.heking.doublecameralive;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
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
 * 调用前后摄像头拍照(同时拍照)
 */
public class DoubleCaptureImageActivity extends AppCompatActivity {
    private static final String TAG = ">>>DoubleCaptureImage";

    private SurfaceView surfaceView;
    private SurfaceView surfaceView2;
    private TextView tvCapture;
    private ImageView ivCancel;
    private ImageView ivOk;
    private ImageView ivResult;
    private View uiCaptureFinish;
    private View uiCapture;

    private CameraManager cameraManager;
    private CameraManager cameraManager2;
    private Point screenPoint;

    private String resultPath;

    private String path1;
    private String path2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.live_activity_double_capture_image);


        findViews();
        initCamera();
    }

    private void findViews() {
        surfaceView = findViewById(R.id.surfaceView);
        surfaceView2 = findViewById(R.id.surfaceView2);
        tvCapture = findViewById(R.id.tv_capture);
        ivCancel = findViewById(R.id.iv_cancel);
        ivOk = findViewById(R.id.iv_ok);
        ivResult = findViewById(R.id.iv_image);
        uiCaptureFinish = findViewById(R.id.ui_capture_finish);
        uiCapture = findViewById(R.id.ui_group);

        ViewGroup.LayoutParams layoutParams = surfaceView2.getLayoutParams();
        layoutParams.width = getScreenSize().x / 3;
        layoutParams.height = layoutParams.width * 4 / 3;
        surfaceView2.setLayoutParams(layoutParams);
        surfaceView2.setZOrderOnTop(true);
        addListener();
    }

    private void initCamera() {
        Log.i(TAG, "initCamera:打开后摄像头");
        cameraManager = new CameraManager();
        cameraManager.setCameraFacing(Camera.CameraInfo.CAMERA_FACING_BACK);
        cameraManager.setSurfaceView(surfaceView);
        cameraManager.setMirror(true);
        cameraManager.setCaptureCallback((code, path) -> {
            if (code == CaptureCallback.SUCCESS) {
                synchronized (this) {
                    path1 = path;
                    merge();
                }
            }

        });

        cameraManager.setCameraEventCallback(new CameraEventCallback() {
            @Override
            public void onOpenCameraError(int errorCode, String msg) {
                Toast.makeText(DoubleCaptureImageActivity.this, "不支持同时开启前后相机", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onOpenCameraSuccess(android.graphics.Camera camera) {

            }
        });
        cameraManager.init();


        Log.i(TAG, "initCamera:打开前摄像头");
        cameraManager2 = new CameraManager();
        cameraManager2.setCameraFacing(Camera.CameraInfo.CAMERA_FACING_FRONT);
        cameraManager2.setSurfaceView(surfaceView2);
        cameraManager2.setMirror(true);
        cameraManager2.setCaptureCallback((code, path) -> {
            if (code == CaptureCallback.SUCCESS) {
                synchronized (this) {
                    path2 = path;
                    merge();
                }
            }

        });
        cameraManager2.setCameraEventCallback(new CameraEventCallback() {
            @Override
            public void onOpenCameraError(int errorCode, String msg) {
                Toast.makeText(DoubleCaptureImageActivity.this, "不支持同时开启前后相机", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onOpenCameraSuccess(android.graphics.Camera camera) {

            }
        });
        cameraManager2.init();


    }

    private void merge() {
        if (path1 != null && path2 != null) {
            Log.d(TAG, "merge:合成");
            Bitmap bitmap1 = BitmapUtils.decodeFile(path1, getScreenSize().x, getScreenSize().y, Bitmap.Config.RGB_565);
            Bitmap bitmap2 = BitmapUtils.decodeFile(path2, getScreenSize().x, getScreenSize().y, Bitmap.Config.RGB_565);
            Log.d(TAG, "merge:bitmap1:" + bitmap1.getWidth() + "," + bitmap1.getHeight());
            Log.d(TAG, "merge:bitmap2:" + bitmap2.getWidth() + "," + bitmap2.getHeight());
            Bitmap bitmap = Utils.drawBitmap(bitmap1, bitmap2);
            keep(bitmap);
            ivResult.setImageBitmap(bitmap);
            uiCapture.setVisibility(View.GONE);
            uiCaptureFinish.setVisibility(View.VISIBLE);
            cameraManager.stopPreview();
            cameraManager2.stopPreview();
            surfaceView2.setVisibility(View.GONE);

        }
    }

    /**
     * 保存
     *
     * @param bitmap
     */
    private void keep(Bitmap bitmap) {
        if (resultPath != null) {
            FileUtils.deleteFile(new File(resultPath));
        }
        File externalFile = FileUtils.getExternalFile(Environment.DIRECTORY_PICTURES, System.currentTimeMillis() + ".jpg");
        resultPath = externalFile.getAbsolutePath();
        IOUtils.keepFile(externalFile.getAbsolutePath(), BitmapUtils.compressQuality(bitmap, 100));
    }

    private void addListener() {
        tvCapture.setOnClickListener(v -> {
            path1 = null;
            path2 = null;
            cameraManager.captureImage();
            cameraManager2.captureImage();
        });

        ivCancel.setOnClickListener(v -> {
            path1 = null;
            path2 = null;
            surfaceView2.setVisibility(View.VISIBLE);
            uiCapture.setVisibility(View.VISIBLE);
            uiCaptureFinish.setVisibility(View.GONE);
            cameraManager.startPreview();
            cameraManager2.startPreview();

        });
        ivOk.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.putExtra("path", resultPath);
            setResult(RESULT_OK, intent);
            finish();
        });

    }

    private Point getScreenSize() {
        if (screenPoint == null) {
            screenPoint = AppUtils.getScreenSize(this);
        }
        return screenPoint;
    }
}
