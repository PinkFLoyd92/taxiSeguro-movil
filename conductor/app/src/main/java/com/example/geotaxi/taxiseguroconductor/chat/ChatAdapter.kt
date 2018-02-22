package com.example.geotaxi.geotaxi.chat

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.example.geotaxi.taxiseguroconductor.R
import com.example.geotaxi.geotaxi.chat.model.ChatList

/**
 * Created by sebas on 2/4/18.
 */
class ChatAdapter(val mContext : Context,
                  var chatListener : OnChatListener?,
                  val chatList: ChatList
                  ): RecyclerView.Adapter<ChatHolder>(){
    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ChatHolder {
        val itemView : View = createChatItemView()
        return ChatHolder(itemView)
    }

    override fun getItemCount(): Int {
        return chatList.chats.size
    }

    override fun onBindViewHolder(holder: ChatHolder?, position: Int) {
        val chat = chatList.chats[position]
        holder?.bindChatWidget(chat)

        (holder?.itemView?.setOnClickListener {
            chatListener?.launchMessageDialog(chat, position)
            true
        })
    }


    private fun createChatItemView(): View {
        val mailItemView = View.inflate(mContext, R.layout.chat_item, null)
        return mailItemView
    }
    interface OnChatListener {
        fun launchMessageDialog(chat: ChatMapped, position: Int)
    }
}
