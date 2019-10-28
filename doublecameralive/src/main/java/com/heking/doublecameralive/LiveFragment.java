package com.heking.doublecameralive;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.alex.livertmppushsdk.LiveManager;
import com.heking.android.zcamera.camera.Utils.CameraUtil;

import java.io.IOException;
import java.util.List;

/**
 * @Author HK-LJJ
 * @Date 2019/9/29
 * @Description TODO
 */
public class LiveFragment extends Fragment implements SurfaceHolder.Callback {
    private static final String TAG = ">>>LiveFragment";
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;

    private static final String RTMP_URL = "rtmp://hk.download.kumiko.cc/live/m";
    private String publishUrl = RTMP_URL;

    private Camera mCamera;
    private CameraInfo mCameraInfo;
    private Parameters mParameters;
    private int mFacing = CameraInfo.CAMERA_FACING_BACK;

    private int HEIGHT = 640;//640
    private int WIDTH = 480;//480
    private int frameRate = 25;//预览帧
    private int bitrate = 100*1024*8;//码率

    private Camera.Size mPreSize;

    private Integer previewFormat = ImageFormat.NV21;
    private int mPreOrientation;

    private LiveManager mLiveManager;

    public LiveFragment() {

    }

    public static LiveFragment newInstance(int cameraFacing, String rtmpUrl) {
        LiveFragment liveFragment = new LiveFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("cameraFacing", cameraFacing);
        bundle.putString("rtmpUrl", rtmpUrl);
        liveFragment.setArguments(bundle);
        return liveFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments != null) {
            mFacing = arguments.getInt("cameraFacing");
            String url = arguments.getString("rtmpUrl", RTMP_URL);
            publishUrl=TextUtils.isEmpty(url) ? RTMP_URL :url;

            Log.d(TAG, "onCreate:mFacing=" + mFacing);
            Log.d(TAG, "onCreate:publishUrl=" + publishUrl);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.live_fragment_live, container, false);
        mSurfaceView = view.findViewById(R.id.sv_live);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.setKeepScreenOn(true);
        mSurfaceHolder.addCallback(this);

        return view;
    }

    private void initCamera() {
        int cameraId = CameraUtil.getCameraIdByFacing(mFacing);
        if (cameraId != -1) {
            try {
                mCamera = Camera.open(cameraId);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "mFacing=" + mFacing);
                toast("不支持同时开启前后相机");
                getActivity().finish();
            }
            mParameters = mCamera.getParameters();
            mCameraInfo = new CameraInfo();
            Camera.getCameraInfo(cameraId, mCameraInfo);
            configParameters();

        } else {
            Log.e(TAG, "没有相机:" + mFacing);
        }
    }

    private void configParameters() {
        Log.i(TAG, "配置Parameters");
        //预览尺寸
        boolean exchange = false;
        if (WIDTH < HEIGHT) {
            exchange = true;
        }
        Camera.Size bestSize = CameraUtil.getBestSize(WIDTH, HEIGHT, mParameters.getSupportedPreviewSizes(), exchange);
        if (bestSize != null) {
            Log.i(TAG, "bestSize:[" + bestSize.width + "," + bestSize.height + "]");
            mParameters.setPreviewSize(bestSize.width, bestSize.height);
        }
        //设置预览角度
        mPreOrientation = CameraUtil.getPreOrientation(getContext(), mCameraInfo, mFacing);
//        mParameters.setRotation(mPreOrientation);
        mCamera.setDisplayOrientation(mPreOrientation);
        Log.i(TAG, "预览角度:" + mPreOrientation);

        //预览编码 优先使用NV21
        List<Integer> PreviewFormats = mParameters.getSupportedPreviewFormats();
        Integer iNV21Flag = 0;
        Integer iYV12Flag = 0;
        for (Integer yuvFormat : PreviewFormats) {
            if (yuvFormat == ImageFormat.YV12) {
                iYV12Flag = ImageFormat.YV12;
            }
            if (yuvFormat == ImageFormat.NV21) {
                iNV21Flag = ImageFormat.NV21;
            }
        }
        if (iNV21Flag != 0) {
            previewFormat = iNV21Flag;
        } else if (iYV12Flag != 0) {
            previewFormat = iYV12Flag;
        }
        mParameters.setPreviewFormat(previewFormat);
        mParameters.setPreviewFrameRate(frameRate);

        //设置对焦模式
        if (CameraUtil.supportFocus(mParameters, Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            mParameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } else if (CameraUtil.supportFocus(mParameters, Parameters.FOCUS_MODE_AUTO)) {
            mParameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);
        }

        mCamera.setParameters(mParameters);
        mCamera.setPreviewCallback(_previewCallback);
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //开始预览
        mCamera.startPreview();
    }

    /**
     * 预览回掉
     */
    private Camera.PreviewCallback _previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (mPreSize == null) {
                mPreSize = camera.getParameters().getPreviewSize();
                Log.i(TAG, "mPreSize:[" + mPreSize.width + "," + mPreSize.height + "]");
            }
            if (mLiveManager != null) {
                mLiveManager.putFrame(data);
            }

        }
    };


    private void initLiveManager() {
        if (mLiveManager == null) {
            mLiveManager = new LiveManager();
            mLiveManager.setDebug(false)
                    .setCameraCodecType(previewFormat)
                    .setFramerate(frameRate)
                    .setBitrate(bitrate)
                    .setPortrait(false)
                    .setRtmpUrl(publishUrl)
                    .setVideoSize(mPreSize.width, mPreSize.height)
                    .setFrontCamera(mFacing == CameraInfo.CAMERA_FACING_FRONT);
        }
    }

    public synchronized void startPublish() {
        initLiveManager();
        if (!mLiveManager.isStart()) {
            mLiveManager.start();
        }
    }

    public synchronized void stopPublish() {
        if (mLiveManager != null && mLiveManager.isStart()) {
            mLiveManager.stop();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated:");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged:");
        initCamera();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed:");
        release();
    }

    public void release() {
        stopPublish();
        CameraUtil.releaseCamera(mCamera);
    }

    public boolean isStart() {
        return mLiveManager != null && mLiveManager.isStart();
    }

    protected void toast(String msg) {
        if (null != getContext())
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

}
