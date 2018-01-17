package com.example.geotaxi.geotaxi.API.endpoints

import android.content.Context
import android.location.Address
import android.os.AsyncTask
import org.osmdroid.bonuspack.location.GeocoderNominatim
import java.io.IOException
import java.util.*

/**
 * Created by dieropal on 17/01/18.
 */
class GeocoderNominatimAPI {

    fun fromLocationName(locationName: String): List<Address> {
        val flnTask = FromLocationNameTask(locationName)
        return flnTask.execute().get()
    }

    //Class that use geocoder Nominatim server in async mode for get location from address name
    private inner class FromLocationNameTask(val locationName: String) : AsyncTask<Context, List<Address>, List<Address>>() {

        override fun doInBackground(vararg params: Context?): List<Address> {
            val geoNominatim = GeocoderNominatim(Locale.getDefault(), System.getProperty("http.agent"))
            //uncomment for use own server
            // geoNominatim.setService(Env.NOMINATIM_SERVER_URL)
            var addresses = listOf<Address>()
            try {
                addresses = geoNominatim.getFromLocationName(locationName,10, -1.97166,
                        -80.08278,-2.31474,
                        -79.46686)
            } catch (e: IOException) {
                return addresses
            }

            return addresses
        }
    }

}