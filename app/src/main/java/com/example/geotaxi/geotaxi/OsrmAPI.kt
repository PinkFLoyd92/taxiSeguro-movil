package com.example.geotaxi.geotaxi

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path


interface OsrmAPI {

    @GET("route/v1/car/{fromLong},{fromLat};{toLong},{toLat}?overview=false&geometries=polyline&steps=true&annotations=true")
    fun getRoute(
            @Path("fromLong") fromLong: String,
            @Path("fromLat") fromLat: String,
            @Path("toLong") toLong: String,
            @Path("toLat") toLat: String): Call<String>
}