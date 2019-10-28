package com.heking.camerademo;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.heking.doublecameralive.CaptureImageActivity;
import com.heking.doublecameralive.DoubleCaptureImageActivity;
import com.heking.doublecameralive.DoubleLiveActivity;
import com.heking.doublecameralive.LiveActivity;
import com.zhoug.android.common.utils.BitmapUtils;
import com.zhoug.android.permission.PermissionManager;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int REQUEST_IMAGE = 101;
    private ImageView image;
    //rtmp://hk.download.kumiko.cc/live/
    //rtmp://36.159.108.125:8007/live/
    //直播服务器地址
    private static String rtmpBaseUrl="rtmp://36.159.108.125:8007/live/";
    //直播推流后置摄像头的url,uid001_fid001_b组成部分为:用户id_企业id_b/f
    private String publishUrlBack =rtmpBaseUrl+"uid001_fid001_b";
    //直播推流前置摄像头的url
    private String publishUrlFront = rtmpBaseUrl+"uid001_fid001_f";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    private void init() {
        image = findViewById(R.id.image);
        findViewById(R.id.btn1).setOnClickListener(this);
        findViewById(R.id.btn2).setOnClickListener(this);
        findViewById(R.id.btn3).setOnClickListener(this);
        findViewById(R.id.btn4).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent();
        switch (v.getId()) {
            case R.id.btn1:
                //TODO 前后摄像头分别拍1张图片然后合成一张
                //权限申请
                new PermissionManager(this)
                        .addPermissions(Manifest.permission.CAMERA,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE)
                        .setCallback((success, denied) -> {
                            if (success) {
                                intent.setClass(this, CaptureImageActivity.class);
                                startActivityForResult(intent, REQUEST_IMAGE);
                            } else {
                                Toast.makeText(this, "请授予权限", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .request();

                break;
            case R.id.btn2://
                //TODO 调用前后摄像头同时拍照然后合成一张,部分手机支持,如小米
                new PermissionManager(this)
                        .addPermissions(Manifest.permission.CAMERA,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE)
                        .setCallback((success, denied) -> {
                            if (success) {
                                intent.setClass(this, DoubleCaptureImageActivity.class);
                                startActivityForResult(intent, REQUEST_IMAGE);
                            } else {
                                Toast.makeText(this, "请授予权限", Toast.LENGTH_SHORT).show();
                            }

                        })
                        .request();


                break;
            case R.id.btn3://直播
                //TODO 先调用前相机拍一张用户头像,然后再用后相机直播
                new PermissionManager(this)
                        .addPermissions(Manifest.permission.CAMERA,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.RECORD_AUDIO)
                        .setCallback((success, denied) -> {
                            if (success) {
                                intent.setClass(this, LiveActivity.class);
                                //传入推流url
                                intent.putExtra(LiveActivity.KEY_URL, publishUrlBack);
                                startActivity(intent);
                            } else {
                                Toast.makeText(this, "请授予权限", Toast.LENGTH_SHORT).show();
                            }

                        })
                        .request();


                break;
            case R.id.btn4://
                //TODO 调用前后摄像头同时直播,部分手机支持,如小米
                new PermissionManager(this)
                        .addPermissions(Manifest.permission.CAMERA,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.RECORD_AUDIO)
                        .setCallback((success, denied) -> {
                            if (success) {
                                intent.setClass(this, DoubleLiveActivity.class);
                                //传入推流url
                                intent.putExtra(DoubleLiveActivity.KEY_URL_BACK, publishUrlBack);
                                intent.putExtra(DoubleLiveActivity.KEY_URL_FRONT, publishUrlFront);
                                startActivity(intent);
                            } else {
                                Toast.makeText(this, "请授予权限", Toast.LENGTH_SHORT).show();
                            }

                        })
                        .request();


                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE && resultCode == RESULT_OK && data != null) {
            final String path = data.getStringExtra("path");
            Toast.makeText(this, "拍照图片路径:" + path, Toast.LENGTH_LONG).show();
            new Thread(() -> {
                final Bitmap bitmap = BitmapUtils.decodeFile(path, 720, 1080, Bitmap.Config.RGB_565);
                runOnUiThread(() -> image.setImageBitmap(bitmap));

            }).start();
        }
    }


}
