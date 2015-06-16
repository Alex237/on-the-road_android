package com.mapzen.valhalla;

import retrofit.http.GET;
import retrofit.http.Headers;
import retrofit.http.Query;
import retrofit.Callback;


public interface RoutingService {
    @GET("/route")
    public void getRoute(@Query("json") String json,
                         @Query("api_key") String apiKey,
                         retrofit.Callback<Result> callback);
        }