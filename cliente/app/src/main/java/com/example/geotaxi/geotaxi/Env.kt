package com.example.geotaxi.geotaxi

class Env {
    companion object {
        const val OSRM_SERVER_URL = "http://192.168.0.107:5000/route/v1/car/"
        const val NOMINATIM_SERVER_URL = "http://192.168.0.107:80/nominatim/"
        const val API_BASE_URL = "http://172.20.133.213:4000/v1/"
        const val API_SOCKET_URL = "http://172.20.133.213:9000"
    }
}