package com.example.geotaxi.geotaxi.chat.view;

import android.support.v4.app.DialogFragment
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.example.geotaxi.geotaxi.chat.ChatAdapter
import com.example.geotaxi.taxiseguroconductor.R


/**
 * Created by sebas on 2/1/18.
 */

class ChatDialog: DialogFragment(){

    interface ChatDialogListener {
        fun onDialogPositiveClick(dialog: DialogFragment)
        fun onDialogNegativeClick(dialog: DialogFragment)
    }

    lateinit var chatDialogListener : ChatDialogListener
    lateinit var chatView : ChatView
    lateinit var btn_close : Button
    lateinit var recyclerView : RecyclerView
    lateinit var chatListener : ChatAdapter.OnChatListener

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.dialog_chat, container)

        recyclerView = view.findViewById(R.id.chat_dialog_recycler)
        btn_close = view.findViewById<Button>(R.id.chat_dialog_close) as Button
        chatView.initRecyclerView(chatListener)
        assignButtonEvents()
        return view
    }

    fun assignButtonEvents() {
        btn_close.setOnClickListener {
            chatDialogListener.onDialogPositiveClick(this)
        }

    }
}
