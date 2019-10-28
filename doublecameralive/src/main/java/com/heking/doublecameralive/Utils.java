package com.heking.doublecameralive;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.AudioManager;

import com.zhoug.android.common.utils.FileUtils;

import java.io.File;

public class Utils {
    /**
     * 画中画
     * @param bitmap1 背景
     * @param bitmap2 前景
     * @return
     */
    public static Bitmap drawBitmap(Bitmap bitmap1,Bitmap bitmap2){
        Bitmap bitmap=Bitmap.createBitmap(bitmap1.getWidth(),bitmap1.getHeight() ,Bitmap.Config.RGB_565);
        Canvas canvas=new Canvas(bitmap);
        canvas.drawBitmap(bitmap1,0 , 0, null);
        Rect rect=new Rect(0,0, bitmap2.getWidth(), bitmap2.getHeight() );

        //内部的图像宽高设置为外部的1/3
        int width=bitmap1.getWidth()/3;
        int height=bitmap2.getHeight()*width/bitmap2.getWidth();
        int margin=00;

//        RectF rect2=new RectF(bitmap1.getWidth()-width-margin,margin,bitmap1.getWidth()-margin,margin+height);
        RectF rect2=new RectF(bitmap.getWidth()-width-margin, bitmap.getHeight()-height, bitmap.getWidth(), bitmap.getHeight());
        canvas.drawBitmap(bitmap2,rect ,rect2 ,null );
        return bitmap;
    }

    /**
     * 默认储存地址
     * @return
     */
    public static String getDefPath(Context context,String name) {
        if(name==null){
            name=System.currentTimeMillis()+".mp4";
        }
//        File file = FileUtils.getExternalFile(Environment.DIRECTORY_MOVIES, name);
        File file = FileUtils.getExternalFile("0video", name);
        if (file != null) {
            File parentFile = file.getParentFile();
            if(!parentFile.exists()){
                parentFile.mkdirs();
            }
            return file.getAbsolutePath();
        } else {
            File cacheDir = context.getCacheDir();
            File folder = new File(cacheDir, "video");
            if (!folder.exists()) {
                if (folder.mkdirs()) {
                    return new File(folder, name).getAbsolutePath();
                }

            }
        }

        return null;
    }

    /**
     * 关闭开始录像和结束录像时的提示音
     * @param context
     * @param open
     * @param audios
     */
    public static void openAudio(Context context,boolean open,int[] audios){
        AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) {
            return ;
        }

        if (null == audios) {
            audios = new int[4];
            audios[0] = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
            audios[1] = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
            audios[2] = audioManager.getStreamVolume(AudioManager.STREAM_RING);
            audios[3] = audioManager.getStreamVolume(AudioManager.STREAM_DTMF);
        }

        if (open) {
            //还原
            audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
            audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, audios[0], 0);
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, audios[1], 0);
            audioManager.setStreamVolume(AudioManager.STREAM_RING, audios[2], 0);
            audioManager.setStreamVolume(AudioManager.STREAM_DTMF, audios[3], 0);

        } else {
            audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
            audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_DTMF, 0, 0);
        }
    }

    public static String getTimerString(int timer) {
        if (timer < 10) {
            return "00:0" + timer;
        } else if (timer < 60) {
            return "00:" + timer;
        } else if (timer < 3600) {
            int m = timer / 60;
            int s = timer % 60;
            String mm = "";
            String ss = "";
            if (m < 10) {
                mm = "0" + m;
            } else {
                mm = "" + m;
            }
            if (s < 10) {
                ss = "0" + s;
            } else {
                ss = "" + s;
            }
            return mm + ":" + ss;
        }
        return "00:00";
    }
}
