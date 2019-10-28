package com.alex.livertmppushsdk;

import android.graphics.ImageFormat;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author HK-LJJ
 * @Date 2019/9/29
 * @Description TODO
 */
public class LiveManager {
    private static final String TAG = ">>>LiveManager";

    private boolean debug = false;

    /**
     * 锁
     */
    private Lock yuvQueueLock = new ReentrantLock();//

    /**
     * 视频帧队列
     */
    private Queue<byte[]> YUVQueue = new LinkedList<>();

    /**
     * 推流管理类
     */
    private RtmpSessionManager mRtmpSessionMgr = null;

    /**
     * 码率
     */
    private int bitrate = 100 * 1024 * 8;

    /**
     * 帧率
     */
    private int framerate = 25;

    /**
     * audio 采样率
     */
    private int samplerate = 22050;

    /**
     * 通道2个 一个视频1个音频
     */
    private final int CHANNEL_NUMBER_DEF = 2;


    /**
     * 录音机
     */
    private AudioRecord mAudioRecorder = null;
    /**
     * 录音缓存
     */
    private byte[] recorderBuffer = null;

    /**
     * 音频编码器
     */
    private FdkAacEncode mAudioEncoder = null;

    private int _fdkaacHandle = 0;

    private boolean portrait = true;

    /**
     * rtmp推流地址
     */
    private String rtmpUrl = "rtmp://hk.download.kumiko.cc/live/m";


    /**
     * 是否是前相机
     */
    private boolean isFrontCamera = false;


    /**
     * 视频编码器
     */
    private SWVideoEncoder mVideoEncoder = null;

    /**
     * 是否在推流
     */
    private volatile boolean start = false;

    private int width = 640;//宽大于高
    private int height = 480;

    /**
     * 旋转后的yuv数据
     */
    private byte[] yuvEdit = new byte[width * height * 3 / 2];


    /**
     * 预览数据帧的编码类型 see {@link android.hardware.Camera.Parameters#setPreviewFormat(int)}
     */
    private Integer cameraCodecType = ImageFormat.NV21;


    /**
     * 预览帧编码为h264的线程
     */
    private Thread mH264EncoderThread = null;

    /**
     * 预览帧编码为h264
     */
    private Runnable mH264Runnable = new Runnable() {
        @Override
        public void run() {
            while (!Thread.interrupted() && start) {
                int iSize = YUVQueue.size();
                if (iSize > 0) {
                    yuvQueueLock.lock();
                    byte[] yuvData = YUVQueue.poll();

                    yuvQueueLock.unlock();
                    if (yuvData == null || yuvData.length <= 0) {
                        continue;
                    }
                    //旋转yuv帧数据

                    if(portrait){
                        if (isFrontCamera) {
                            yuvEdit = mVideoEncoder.YUV420pRotate270(yuvData, width, height);
                        } else {
                            yuvEdit = mVideoEncoder.YUV420pRotate90(yuvData, width, height);
                        }
                    }else{
                        if (isFrontCamera) {
                            yuvEdit =yuvData;
                        } else {
                            yuvEdit =yuvData;
                        }
                    }



                    //yuv->h264
                    byte[] h264Data = mVideoEncoder.EncoderH264(yuvEdit);
                    if (h264Data != null) {
                        //添加视频帧
                        if (debug) {
                            Log.d(TAG, "encode h264Data. video:[" + width + "," + height + "]" + ";length=" + (h264Data.length / 1024) + "k");
                        }
                        mRtmpSessionMgr.InsertVideoData(h264Data);
                    }
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
//                    e.printStackTrace();
                }
            }
            if (YUVQueue != null)
                YUVQueue.clear();
            Log.i(TAG, "结束h264编码线程");

        }
    };

    /**
     * 音频编码为aac线程
     */
    private Thread mAacEncoderThread = null;

    /**
     * 音频编码为aac
     */
    private Runnable mAacEncoderRunnable = new Runnable() {
        @Override
        public void run() {
            long lSleepTime = samplerate * 16 * 2 / recorderBuffer.length;

            while (!Thread.interrupted() && start) {
                int iPCMLen = mAudioRecorder.read(recorderBuffer, 0, recorderBuffer.length); // Fill buffer
                if ((iPCMLen != mAudioRecorder.ERROR_BAD_VALUE) && (iPCMLen != 0)) {
                    if (_fdkaacHandle != 0) {
                        byte[] aacBuffer = mAudioEncoder.FdkAacEncode(_fdkaacHandle, recorderBuffer);
                        if (aacBuffer != null) {
                            if (debug) {
                                Log.d(TAG, "audio aacBuffer:length=" + (aacBuffer.length / 1024) + "k");
                            }
                            mRtmpSessionMgr.InsertAudioData(aacBuffer);
                        }
                    }
                } else {
                    Log.i(TAG, "######fail to get PCM data");
                }
                try {
                    Thread.sleep(lSleepTime / 10);
                } catch (InterruptedException e) {
//                    e.printStackTrace();
                }
            }
            Log.i(TAG, "结束音频编码线程");
        }
    };


    /**
     * 初始化 录音机
     */
    private void initAudioRecord() {
        int recorderBufferSize = AudioRecord.getMinBufferSize(samplerate,
                AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);
        mAudioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                samplerate, AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, recorderBufferSize);
        recorderBuffer = new byte[recorderBufferSize];

        mAudioEncoder = new FdkAacEncode();
        _fdkaacHandle = mAudioEncoder.FdkAacInit(samplerate, 2);
    }


    /**
     * 放入预览帧数据
     *
     * @param data
     */
    public void putFrame(byte[] data) {
        if (!start || data == null) {
            return;
        }
        if (debug) {
            Log.d(TAG, "putFrame:data.size=" + (data.length / 1024) + "k");
        }
        byte[] yuv420 = null;

        if (cameraCodecType == ImageFormat.YV12) {
            yuv420 = new byte[data.length];
            mVideoEncoder.swapYV12toI420_Ex(data, yuv420, width, height);
        } else if (cameraCodecType == ImageFormat.NV21) {
            yuv420 = mVideoEncoder.swapNV21toI420(data, width, height);
        }

        if (yuv420 == null) {
            return;
        }

        yuvQueueLock.lock();
        if (YUVQueue.size() >= 10) {
            YUVQueue.poll();
            if (debug) {
                Log.d(TAG, "丢帧");
            }
        }
        YUVQueue.offer(yuv420);
        if (debug) {
            Log.d(TAG, "putFrame:yuv420.size=" + (yuv420.length / 1024) + "k");
        }
        yuvQueueLock.unlock();
    }

    /**
     * 开始推流
     */
    public synchronized void start() {
        if (start) {
            Log.e(TAG, "已经开始了");
            return;
        }

        start = true;
        Log.i(TAG, "开始推流:");
        //rtmp推流管理类
        mRtmpSessionMgr = new RtmpSessionManager();
        mRtmpSessionMgr.Start(rtmpUrl);

        //启动video编码器
        int iFormat = cameraCodecType;
        if(portrait){
            mVideoEncoder = new SWVideoEncoder(height, width, framerate, bitrate);
        }else{
            mVideoEncoder = new SWVideoEncoder(width, height, framerate, bitrate);
        }
        mVideoEncoder.start(iFormat);

        //启动h264编码线程
        mH264EncoderThread = new Thread(mH264Runnable);
        mH264EncoderThread.setPriority(Thread.MAX_PRIORITY);
        mH264EncoderThread.start();

        //初始录音机
        initAudioRecord();
        mAudioRecorder.startRecording();

        //启动aac编码线程
        mAacEncoderThread = new Thread(mAacEncoderRunnable);
        mAacEncoderThread.setPriority(Thread.MAX_PRIORITY);
        mAacEncoderThread.start();

    }

    /**
     * 结束推流
     */
    public synchronized void stop() {
        if (!start) {
            Log.e(TAG, "已经停止了");
            return;
        }
        start = false;
        Log.i(TAG, "结束推流:");

        mAacEncoderThread.interrupt();
        mH264EncoderThread.interrupt();

        mAudioRecorder.stop();
        mVideoEncoder.stop();

        mRtmpSessionMgr.Stop();

        yuvQueueLock.lock();
        YUVQueue.clear();
        yuvQueueLock.unlock();

    }


    public LiveManager setBitrate(int bitrate) {
        this.bitrate = bitrate;
        return this;
    }

    public LiveManager setSamplerate(int samplerate) {
        this.samplerate = samplerate;
        return this;
    }

    public LiveManager setFramerate(int framerate) {
        this.framerate = framerate;
        return this;

    }

    public LiveManager setRtmpUrl(String rtmpUrl) {
        this.rtmpUrl = rtmpUrl;
        return this;

    }

    public LiveManager setFrontCamera(boolean frontCamera) {
        isFrontCamera = frontCamera;
        return this;
    }

    public LiveManager setCameraCodecType(Integer cameraCodecType) {
        this.cameraCodecType = cameraCodecType;
        return this;
    }

    public LiveManager setVideoSize(int width, int height) {
        this.width = width;
        this.height = height;
        yuvEdit = new byte[width * height * 3 / 2];

        return this;
    }

    public boolean isStart() {
        return start;
    }

    public LiveManager setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public LiveManager setPortrait(boolean portrait) {
        this.portrait = portrait;
        return this;
    }

}
