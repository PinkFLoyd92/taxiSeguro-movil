package com.example.geotaxi.geotaxi.chat

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import com.example.geotaxi.geotaxi.chat.model.ChatList

/**
 * Created by sebas on 2/4/18.
 */
class ChatRecyclerView(val recyclerView: RecyclerView,
                       chatEventListener: ChatAdapter.OnChatListener?,
                        val chatList: ChatList
                       )  {

    val ctx: Context = recyclerView.context
    private val chatAdapter = ChatAdapter(ctx, chatEventListener, chatList)

    init {
        recyclerView.layoutManager = LinearLayoutManager(ctx)
        recyclerView.adapter = chatAdapter
    }

    fun setChatListener(chatEventListener: ChatAdapter.OnChatListener?){
        chatAdapter.chatListener = chatEventListener
    }

    fun notifyChatSetChanged() {
        chatAdapter.notifyDataSetChanged()
    }

    fun notifyChatRangeInserted(positionStart: Int, itemCount: Int) {
        chatAdapter.notifyItemRangeInserted(positionStart, itemCount)
    }

    fun notifyChatChanged(position: Int) {
        //labelThreadAdapter.notifyItemChanged(position)
        chatAdapter.notifyDataSetChanged()
    }

    fun notifyChatRemoved(position: Int) {
        chatAdapter.notifyItemRemoved(position)
    }
}
