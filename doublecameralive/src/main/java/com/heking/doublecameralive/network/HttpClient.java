package com.heking.doublecameralive.network;

import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * @Author HK-LJJ
 * @Description TODO
 */
public class HttpClient {
    private static final String TAG = ">>>>>HttpClient";
    public static final String FileType="application/otcet-stream";
    public static final String JsonType="application/json";

    private static HttpClient mInstance;//单例
    private  Retrofit mRetrofit;//单例
    private  HttpRequest mRequest;//网络请求方法类 单例
    private static int connTimeOut=10;//秒
    private static int readTimeOut=60*1;//秒
    private static boolean DEBUG=false;

    private HttpClient() {
        initRequest();
    }

    /**
     * 单例模式
     * @return HttpClient
     */
    public static HttpClient getInstance(){
        if(mInstance==null){
            synchronized (HttpClient.class){
                if(mInstance==null){
                    mInstance=new HttpClient();
                    Log.i(TAG, "初始化HttpClient");
                }
            }
        }
        return mInstance;
    }

    private void initRequest(){
        if(mRequest==null){
            synchronized (HttpClient.class){
                if(mRequest==null){
                    if(mRetrofit==null){
                        initRetrofit();
                    }
                    mRequest=mRetrofit.create(HttpRequest.class);
                    Log.i(TAG, "初始化Request完成");
                }
            }
        }
    }

    private void initRetrofit(){
        mRetrofit = new Retrofit.Builder()
                .client(initOkHttp())
                .baseUrl(HttpRequest.BaseUrl)
                .addConverterFactory(GsonConverterFactory.create())//json字符串转化为对象
                .build();
        Log.i(TAG, "初始化mRetrofit完成");
    }

    private OkHttpClient initOkHttp(){
        HttpLoggingInterceptor httpLogging=new HttpLoggingInterceptor();

        if(DEBUG){
            httpLogging.setLevel(HttpLoggingInterceptor.Level.BODY);
        }else{
            httpLogging.setLevel(HttpLoggingInterceptor.Level.NONE);
        }

        //网络请求框架
        OkHttpClient okHttp = new OkHttpClient.Builder()
                .addInterceptor(httpLogging)
                .connectTimeout(connTimeOut, TimeUnit.SECONDS)//连接超时10秒
                .readTimeout(readTimeOut,TimeUnit.SECONDS)
                .build();
        Log.i(TAG, "初始化okHttp完成");
        return okHttp;
    }

    public HttpRequest getRequest() {
        return mRequest;
    }

    public static Call<ResponseBody> post(String relativeUrl, Map<String,Object> fields, List<String> paths){
        //参数
        Map<String, RequestBody> params=new HashMap<>();
        if (fields != null && fields.size() > 0) {
            for (Map.Entry<String, Object> next : fields.entrySet()) {
                params.put(next.getKey(), RequestBody.create(MediaType.parse(JsonType), next.getValue().toString()));
            }
        }

        //文件
        List<MultipartBody.Part> files=new ArrayList<>();
        if (paths != null && paths.size() > 0) {
            for (int i = 0; i < paths.size(); i++) {
                String path = paths.get(i);
                RequestBody requestBody = RequestBody.create(MediaType.parse(FileType), new File(path));
                MultipartBody.Part file1 = MultipartBody.Part.createFormData("file" + i,new File(path).getName(), requestBody);
                files.add(file1);
            }
        }

        return HttpClient.getInstance().getRequest().post(relativeUrl, params, files);
    }
}
