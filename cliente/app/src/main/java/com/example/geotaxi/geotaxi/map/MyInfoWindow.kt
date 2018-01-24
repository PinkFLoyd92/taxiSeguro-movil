package com.example.geotaxi.geotaxi.map

import android.widget.TextView
import com.example.geotaxi.geotaxi.R
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.infowindow.InfoWindow

/**
 * Created by dieropal on 23/01/18.
 */
class MyInfoWindow(layoutResId: Int, mapView: MapView,
                   title: String, description: String): InfoWindow(layoutResId, mapView) {
    init {
        mView.findViewById<TextView>(R.id.time).text = title
        //mView.findViewById<TextView>(R.id.bubble_description).text = description
    }

    override fun onOpen(item: Any?) {

    }

    override fun onClose() {

    }

}