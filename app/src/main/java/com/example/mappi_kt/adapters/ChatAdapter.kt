package com.example.mappi_kt.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mappi_kt.R
import com.example.mappi_kt.entity.ChatItem

class ChatAdapter(
    private val context: Context,
    private val itemClickListener: OnChatItemClickListener
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    private var chatItems: List<ChatItem>? = null

    interface OnChatItemClickListener {
        fun onChatItemClick(position: Int)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setChatItems(chatItems: List<ChatItem>) {
        this.chatItems = chatItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.chat_item, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chatItem = chatItems?.get(position)
        chatItem?.let {
            holder.userNameTextView.text = it.userName
            holder.messageTextView.text = it.lastMessage
            holder.timestampTextView.text = it.lastTimestamp

            Glide.with(context)
                .load(it.userImageRes)
                .placeholder(R.drawable.ic_person_foreground)
                .error(R.drawable.ic_person_foreground)
                .into(holder.userImageView)
        }

        holder.itemView.setOnClickListener {
            itemClickListener.onChatItemClick(position)
        }
    }

    override fun getItemCount(): Int {
        return chatItems?.size ?: 0
    }

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userNameTextView: TextView = itemView.findViewById(R.id.chatUserNameTextView)
        val messageTextView: TextView = itemView.findViewById(R.id.chatMessageTextView)
        val timestampTextView: TextView = itemView.findViewById(R.id.chatTimestampTextView)
        val userImageView: ImageView = itemView.findViewById(R.id.userImageView)
    }
}