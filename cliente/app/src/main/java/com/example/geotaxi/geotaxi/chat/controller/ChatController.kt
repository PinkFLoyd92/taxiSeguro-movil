package com.example.geotaxi.geotaxi.chat.controller

import com.example.geotaxi.geotaxi.chat.ChatAdapter
import com.example.geotaxi.geotaxi.chat.ChatMapped
import com.example.geotaxi.geotaxi.chat.model.ChatList
import com.example.geotaxi.geotaxi.chat.view.ChatView
import com.example.geotaxi.geotaxi.chat.view.MessageDialog
import com.example.geotaxi.geotaxi.data.Chat
import com.example.geotaxi.geotaxi.data.Route
import com.example.geotaxi.geotaxi.ui.MainActivity

/**
 * Created by sebas on 2/4/18.
 */

class ChatController (val chatScene: ChatView.ChatScene,
                      val activity: MainActivity,
                      val chatList: ChatList) {

    private val chatListener = object : ChatAdapter.OnChatListener{

        override fun launchMessageDialog(chat: ChatMapped, position: Int) {
            val messageDialog:MessageDialog = MessageDialog()

            messageDialog.show(activity.fragmentManager, "")
        }

        fun onBackPressed(mainActivity: MainActivity) {
            TODO("not implemented")
        }
    }

    fun onStart() {
        chatScene.attachView(chatListener)
    }

    fun isMonitorAlreadyCreated(id: String): Boolean {
        val chatMapped: ChatMapped? = chatList.chats.find {
            it.id == id
        }
        if(chatMapped != null) {
            return true
        }
        return false
    }

    fun addMonitor(id_user: String, username: String, role: String) {
        val chat: Chat = Chat(id_user = id_user,
                id_route = Route.instance._id,
                username = username,
                role = role
                )
        chatList.chats.add(ChatMapped(chat))
        try {
            chatScene.notifyChatSetChanged()
        }catch (e: kotlin.UninitializedPropertyAccessException) {

        }
    }
}
