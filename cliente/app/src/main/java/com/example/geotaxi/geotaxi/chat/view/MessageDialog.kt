package com.example.geotaxi.geotaxi.chat.view

import android.app.DialogFragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.geotaxi.geotaxi.R

/**
 * Created by sebas on 2/5/18.
 */
class MessageDialog: DialogFragment() {
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.dialog_messages, container)

        return view
    }

}
