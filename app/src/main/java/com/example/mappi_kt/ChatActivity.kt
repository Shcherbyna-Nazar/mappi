package com.example.mappi_kt

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.mappi_kt.adapters.MessageAdapter
import com.example.mappi_kt.entity.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity() {
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private var messageList: MutableList<Message> = mutableListOf()
    private lateinit var currentUserMessagesRef: DatabaseReference
    private lateinit var friendMessagesRef: DatabaseReference
    private lateinit var currentUserUid: String
    private lateinit var friendUid: String
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: ImageView
    private lateinit var friendUserName: TextView
    private lateinit var friendImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize the message list
        messageList = ArrayList()

        // Get the current user's UID
        val currentUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser
        currentUserUid = currentUser?.uid ?: ""

        friendImageView = findViewById(R.id.friendImageView)
        friendUserName = findViewById(R.id.friendUserName)

        val imageUrl = intent.getStringExtra("userImage")
        friendUserName.text = intent.getStringExtra("friendUserName")

        val requestOptions = RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL) // Caches both the original and resized image

        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .apply(requestOptions)
                .into(friendImageView)
        } else {
            // Set a default image when the URL is null or empty
            friendImageView.setImageResource(R.drawable.ic_person_foreground)
        }

        // Get the friend's UID from the intent
        friendUid = intent.getStringExtra("friendUid") ?: ""

        // Get the reference to the messages node in the Firebase Database for both perspectives
        currentUserMessagesRef = FirebaseDatabase.getInstance().reference
            .child("chat")
            .child(currentUserUid)
            .child(friendUid)
            .child("messages")

        friendMessagesRef = FirebaseDatabase.getInstance().reference
            .child("chat")
            .child(friendUid)
            .child(currentUserUid)
            .child("messages")

        val valueEventListener = object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                messageList.clear()
                for (messageSnapshot in dataSnapshot.children) {
                    val message = messageSnapshot.getValue(Message::class.java)
                    if (message != null) {
                        message.sent = (!message.receiverUid.equals(currentUserUid))
                        messageList.add(message)
                    }
                }

                // Sort the message list based on date and timestamp
                messageList.sortWith(compareBy(Message::date, Message::timestamp))

                // Update the messageAdapter with the sorted message list
                messageAdapter.notifyDataSetChanged()

                // Scroll only if the messageList is not empty
                if (messageList.isNotEmpty()) {
                    chatRecyclerView.smoothScrollToPosition(messageList.size - 1)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle any errors that occur during data retrieval
            }
        }

        currentUserMessagesRef.addValueEventListener(valueEventListener)

        // Set up the adapter
        messageAdapter = MessageAdapter(messageList)
        chatRecyclerView.adapter = messageAdapter

        // Set up messageEditText and sendButton
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendMessage)
        sendButton.setOnClickListener {
            val messageText = messageEditText.text.toString().trim()
            if (!TextUtils.isEmpty(messageText)) {
                // Create a new message object
                val currentTimestamp = MessageAdapter.getCurrentTimestamp()
                val date = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date())


                val newMessage = Message(
                    messageText,
                    currentTimestamp,
                    date,
                    true,
                    currentUserUid,
                    friendUid
                )

                // Save the message to Firebase
                val newMessageKey = currentUserMessagesRef.push().key
                newMessageKey?.let {
                    currentUserMessagesRef.child(it).setValue(newMessage)
                    friendMessagesRef.child(it).setValue(newMessage)
                }

                // Clear the input field
                messageEditText.setText("")
            }
        }
    }
}
