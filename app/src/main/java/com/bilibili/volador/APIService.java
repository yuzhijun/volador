package com.bilibili.volador;


import com.bilibili.engine_center.net.BaseUrl;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

@BaseUrl("http://sportsnba.qq.com/")
public interface APIService {

    @GET("news/item")
    Call<ResponseBody> getNBANews(@Query("column") String column,
                                  @Query("articleIds") String articleIds);

}