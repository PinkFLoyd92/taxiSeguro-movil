package com.example.geotaxi.geotaxi.chat.controller

import android.app.DialogFragment
import co.intentservice.chatui.models.ChatMessage
import com.example.geotaxi.geotaxi.chat.ChatAdapter
import com.example.geotaxi.geotaxi.chat.ChatMapped
import com.example.geotaxi.geotaxi.chat.model.ChatList
import com.example.geotaxi.geotaxi.chat.view.ChatView
import com.example.geotaxi.geotaxi.chat.view.MessageDialog
import com.example.geotaxi.geotaxi.data.Chat
import com.example.geotaxi.geotaxi.data.Route
import com.example.geotaxi.geotaxi.socket.SocketIOClientHandler
import com.example.geotaxi.geotaxi.ui.MainActivity

/**
 * Created by sebas on 2/4/18.
 */

class ChatController (val chatScene: ChatView.ChatScene,
                      val activity: MainActivity,
                      val chatList: ChatList
    ) {
    lateinit var socketHandler : SocketIOClientHandler
    private val instance :ChatController = this
    var messageDialog: MessageDialog = MessageDialog()

    private val chatListener = object : ChatAdapter.OnChatListener{

        override fun launchMessageDialog(chat: ChatMapped, position: Int) {
            messageDialog.chatController = instance
            chatList.selectedChat = chat
            messageDialog.show(activity.fragmentManager, "")
        }

        fun onBackPressed(mainActivity: MainActivity) {
            TODO("not implemented")
        }
    }

    fun getMessages() : ArrayList<ChatMessage> {
        return chatList.selectedChat.messages
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

    fun tryToAddMessage(chatMessage: ChatMessage) {
        try {
            messageDialog.renderMessage(chatMessage)
        } catch (e: Exception){
            e.printStackTrace()
        }
    }
    fun addMonitor(id_user: String, username: String, role: String) {
        val chatMapTmp: ChatMapped? = chatList.chats.find {
            it.monitor_id == id_user
        }
        if(chatMapTmp != null) return
        val chat = Chat(id_user = id_user,
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

    fun removeMonitor(id_user: String) {
        val chatMapTmp: ChatMapped? = chatList.chats.find {
            it.monitor_id == id_user
        }
        if(chatMapTmp == null) return

        chatList.chats.remove(chatMapTmp)
        try {
            chatScene.notifyChatSetChanged()
        }catch (e: kotlin.UninitializedPropertyAccessException) {

        }
    }

    fun sendMessage(chatMessage: ChatMessage) {
        socketHandler.emitMessage(chatMessage)
    }

    fun addOwnMessage(chatMessage: ChatMessage) {
        this.chatList.selectedChat.messages.add(chatMessage)
    }
}
