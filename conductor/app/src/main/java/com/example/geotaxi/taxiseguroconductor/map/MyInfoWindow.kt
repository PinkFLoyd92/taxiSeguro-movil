package com.example.geotaxi.taxiseguroconductor.map

import android.view.View
import android.widget.TextView
import com.example.geotaxi.taxiseguroconductor.R
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.infowindow.InfoWindow

/**
 * Created by dieropal on 25/01/18.
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

    fun setTittle(tittle: String) {
        mView.findViewById<TextView>(R.id.route_tittle).text = tittle
    }

    fun setTittleColor(color: Int){
        mView.findViewById<TextView>(R.id.route_tittle).setTextColor(color)
    }
    fun showTittle() {
        mView.findViewById<TextView>(R.id.route_tittle).visibility = View.VISIBLE
    }

    fun hideTittle() {
        mView.findViewById<TextView>(R.id.route_tittle).visibility = View.GONE
    }

}