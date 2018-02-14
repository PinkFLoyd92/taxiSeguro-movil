package com.example.geotaxi.geotaxi.API.endpoints

import android.content.Context
import android.location.Address
import android.os.AsyncTask
import com.example.geotaxi.geotaxi.config.Env
import org.osmdroid.bonuspack.location.GeocoderNominatim
import java.io.IOException
import java.util.*

/**
 * Created by dieropal on 17/01/18.
 */
class GeocoderNominatimAPI {
    val LOWER_LEFT_LATITUDE = -1.97166
    val LOWER_LEFT_LONGITUDE = -80.08278
    val UPPER_RIGHT_LATITUDE = -2.31474
    val UPPER_RIGHT_LONGITUDE = -79.46686
    val MAX_RESULTS = 10
    fun fromLocationName(locationName: String): List<Address> {
        val flnTask = FromLocationNameTask(locationName)
        return flnTask.execute().get()
    }

    //Class that use geocoder Nominatim server in async mode for get location from address name
    private inner class FromLocationNameTask(val locationName: String) : AsyncTask<Context, List<Address>, List<Address>>() {

        override fun doInBackground(vararg params: Context?): List<Address> {
            val geoNominatim = GeocoderNominatim(Locale.getDefault(), System.getProperty("http.agent"))
            //uncomment for use own server
            geoNominatim.setService(Env.NOMINATIM_SERVER_URL)
            var addresses = listOf<Address>()
            try {
                addresses = geoNominatim.getFromLocationName(locationName,MAX_RESULTS,
                        LOWER_LEFT_LATITUDE, LOWER_LEFT_LONGITUDE,
                        UPPER_RIGHT_LATITUDE, UPPER_RIGHT_LONGITUDE)
            } catch (e: IOException) {
                return addresses
            }

            return addresses
        }
    }

}