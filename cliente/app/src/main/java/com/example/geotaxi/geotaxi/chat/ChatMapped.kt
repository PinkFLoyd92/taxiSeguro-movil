package com.example.geotaxi.geotaxi.chat

import com.example.geotaxi.geotaxi.data.Chat

/**
 * Created by sebas on 2/4/18.
 */

class ChatMapped(val chat : Chat) {
    val chatIdRoute = chat.id_route
    val monitorInfo = chat.username
    val chatRole = chat.role
    val messages = chat.messages
    val id : String
    get() {
        return chat.id_user
    }
}
