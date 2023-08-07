package com.example.mappi_kt.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mappi_kt.R
import com.example.mappi_kt.entity.Message
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(private val messageList: List<Message>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view: View = if (viewType == 0) {
            LayoutInflater.from(parent.context).inflate(R.layout.send_message, parent, false)
        } else {
            LayoutInflater.from(parent.context).inflate(R.layout.recieve_message, parent, false)
        }
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message: Message = messageList[position]
        holder.messageTextView.text = message.message
        holder.timestampTextView.text =
            message.timestamp?.format("hh:mm a", Locale.getDefault()) ?: ""
    }

    override fun getItemViewType(position: Int): Int {
        val message: Message = messageList[position]
        return if (message.sent) {
            0 // Sent message
        } else {
            1 // Received message
        }
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
        val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
    }

    companion object {
        fun getCurrentTimestamp(): String {

            return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        }
    }
}