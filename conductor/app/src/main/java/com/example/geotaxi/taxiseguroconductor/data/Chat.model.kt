package com.example.geotaxi.taxiseguroconductor.data

import co.intentservice.chatui.models.ChatMessage

/**
 * Created by sebas on 2/4/18.
 */

class Chat(id_route: String, id_user: String, role:String, username: String) {
    var id_route : String
    var id_user : String // Usuario con el que esta chateando... (monitor)
    var username : String
    var role : String
    val messages: ArrayList<ChatMessage> = ArrayList()

    init {
        this.id_route = id_route
        this.id_user = id_user
        this.role = role
        this.username = username
    }
}