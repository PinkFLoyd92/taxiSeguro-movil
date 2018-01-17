package com.example.geotaxi.geotaxi.ui.adapter

import android.location.Address
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.geotaxi.geotaxi.R

class AddressListViewAdapter(dataset: List<Address>, rVClickListener: View.OnClickListener) : RecyclerView.Adapter<AddressListViewAdapter.Companion.ViewHolder>() {

    var mDataset = dataset
    val mClickListener = rVClickListener

    // Provide a reference to the views for each data item
    // you provide access to all the views for a data item in a view holder
    companion object {
         class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
             val mLayoutView = v

             fun setAddress(address: String) {
                 mLayoutView.findViewById<TextView>(R.id.address_tv).text = address
             }

             fun setLocation(location: String) {
                 mLayoutView.findViewById<TextView>(R.id.location_tv).text = location
             }

             fun setClickListener(cl: View.OnClickListener) {
                 val cardView = mLayoutView.findViewById<CardView>(R.id.address_item_card_view)
                 cardView.setOnClickListener(cl)
             }
         }
    }

    override fun getItemCount(): Int {
        return mDataset.size
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        // create a new view
        val v = LayoutInflater.from(parent?.context)
                .inflate(R.layout.address_item_list, parent, false) as View

        return ViewHolder(v)

    }

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        val address = mDataset[position].extras["display_name"].toString()
        val location = mDataset[position].latitude.toString() + ", " +
                             mDataset[position].longitude.toString()
        holder?.setAddress(address)
        holder?.setLocation(location)
        holder?.setClickListener(mClickListener)
    }

}