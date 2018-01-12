package com.example.geotaxi.geotaxi

class Env {
    companion object {
        const val IP = "http://192.168.43.138"
        const val OSRM_SERVER_URL = IP+":5000/route/v1/car/"
        const val NOMINATIM_SERVER_URL = IP+":80/nominatim/"
        const val API_BASE_URL = IP+":4000/v1/"
        const val API_SOCKET_URL = IP+":9000"
    }
}