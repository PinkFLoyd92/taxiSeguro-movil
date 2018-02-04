package com.example.geotaxi.geotaxi.chat

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.example.geotaxi.geotaxi.R

/**
 * Created by sebas on 2/4/18.
 */

class ChatHolder(val view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {

    private val monitorView : TextView

    init {
        view.setOnClickListener(this)
    }

    override fun onClick(p0: View?) {
    }

    fun bindChatWidget(chatWidget: ChatMapped) {
        monitorView.text = chatWidget.monitorInfo
    }

    init {
        monitorView = view.findViewById<TextView>(R.id.chat_item_monitor)
    }
}
