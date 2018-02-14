package com.example.geotaxi.geotaxi.chat.view

import android.support.v7.widget.RecyclerView
import android.widget.TextView
import com.example.geotaxi.geotaxi.R
import com.example.geotaxi.geotaxi.chat.ChatAdapter
import com.example.geotaxi.geotaxi.chat.ChatRecyclerView
import com.example.geotaxi.geotaxi.chat.model.ChatList
import com.example.geotaxi.geotaxi.ui.MainActivity
import kotlinx.android.synthetic.main.nav_body.view.*
import org.w3c.dom.Text

/**
 * Created by sebas on 2/4/18.
 */

interface ChatView {
    fun getDialog() : ChatDialog
    fun renderMessageCount()
    fun initRecyclerView(chatListener: ChatAdapter.OnChatListener)

    class ChatScene(val activity: MainActivity, val chatList: ChatList): ChatView{

        lateinit var recyclerView: RecyclerView
        val drawerCounter = activity.findViewById<TextView>(R.id.count_inbox)

        val countMessages: Int
            get() = chatList.selectedChat.countMessages

        lateinit var chatRecyclerView: ChatRecyclerView

        var chatDialog : ChatDialog = ChatDialog()

        override fun initRecyclerView(chatListener: ChatAdapter.OnChatListener) {
            recyclerView = getDialog().recyclerView
            chatRecyclerView = ChatRecyclerView(recyclerView, chatListener, chatList)
        }

        override fun renderMessageCount() {
            drawerCounter.text = countMessages.toString()
        }

        override fun getDialog(): ChatDialog {
            return chatDialog
        }

        fun attachView(chatListener: ChatAdapter.OnChatListener) {
            bindChatDialog(chatListener)
            chatDialog.show(activity.supportFragmentManager, "")
        }
        fun notifyChatSetChanged() {
            chatRecyclerView.notifyChatSetChanged()
        }

        fun notifyChatRemoved(position: Int) {
            chatRecyclerView.notifyChatRemoved(position)
        }

        fun notifyChatRangeInserted(positionStart: Int, itemCount: Int) {
            chatRecyclerView.notifyChatRangeInserted(positionStart, itemCount)
        }

        fun bindChatDialog(chatEventListener: ChatAdapter.OnChatListener) {
            getDialog().chatView = this
            getDialog().chatListener = chatEventListener
            getDialog().chatDialogListener = activity
        }
    }
}