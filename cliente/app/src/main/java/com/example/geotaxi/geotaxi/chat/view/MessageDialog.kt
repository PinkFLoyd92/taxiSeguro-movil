package com.example.geotaxi.geotaxi.chat.view

import android.app.DialogFragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import co.intentservice.chatui.ChatView
import com.example.geotaxi.geotaxi.R
import com.example.geotaxi.geotaxi.chat.controller.ChatController
import co.intentservice.chatui.models.ChatMessage



/**
 * Created by sebas on 2/5/18.
 */
class MessageDialog: DialogFragment() {

    lateinit var chatController: ChatController
    lateinit var chat_ui : ChatView
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.dialog_messages, container)
        chat_ui = view.findViewById(R.id.chat_view)
        chat_ui.addMessages(chatController.getMessages())
        chat_ui.setOnSentMessageListener(ChatView.OnSentMessageListener {
            chatController.addOwnMessage(it)
            chatController.sendMessage(it)
            true
        })
        return view
    }
    fun renderMessage(chatMessage: ChatMessage){
        chatController.activity.runOnUiThread {
            chat_ui.addMessage(chatMessage)
        }
    }
}
