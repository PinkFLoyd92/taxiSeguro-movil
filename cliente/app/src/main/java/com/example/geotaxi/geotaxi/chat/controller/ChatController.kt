package com.example.geotaxi.geotaxi.chat.controller

import com.example.geotaxi.geotaxi.chat.ChatAdapter
import com.example.geotaxi.geotaxi.chat.ChatMapped
import com.example.geotaxi.geotaxi.chat.model.ChatList
import com.example.geotaxi.geotaxi.chat.view.ChatView
import com.example.geotaxi.geotaxi.ui.MainActivity

/**
 * Created by sebas on 2/4/18.
 */

class ChatController (val chatScene: ChatView.ChatScene,
                      val activity: MainActivity,
                      val chatList: ChatList) {

    private val chatListener = object : ChatAdapter.OnChatListener{
        override fun launchMessageDialog(chat: ChatMapped, position: Int) {
            TODO("CREAR EL MESSAGE DIALOG AQUI.")
        }

        fun onBackPressed(mainActivity: MainActivity) {
            TODO("not implemented")
        }
    }

    fun onStart() {
        chatScene.attachView(chatListener)
    }
}
