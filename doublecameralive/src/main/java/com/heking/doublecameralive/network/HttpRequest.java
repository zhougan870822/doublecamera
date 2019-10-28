package com.heking.doublecameralive.network;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Path;

/**
 * @Author HK-LJJ
 * @Date 2019/10/22
 * @Description TODO
 */
public interface HttpRequest {
    //api服务器地址
    String BaseUrl="http://36.159.108.130:8016/QlYMoblieAPI/";
    //上传直播头像的api地址
    String uploadLivePhoto="FirmInfo/SavePhotographyInfo";

    //表单提交
    @Multipart
    @POST("{url}")
    Call<ResponseBody> post(@Path("url") String url, @PartMap() Map<String, RequestBody> params, @Part List<MultipartBody.Part> files);


}
