package com.example.mappi_kt.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mappi_kt.ChatActivity
import com.example.mappi_kt.R
import com.example.mappi_kt.adapters.ChatAdapter
import com.example.mappi_kt.entity.ChatItem
import com.example.mappi_kt.entity.Message
import com.example.mappi_kt.entity.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChatFragment : Fragment(), ChatAdapter.OnChatItemClickListener {
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private var chatItemList: MutableList<ChatItem> = mutableListOf()

    private lateinit var friendsRef: DatabaseReference
    private lateinit var friendsListener: ValueEventListener

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatRecyclerView = view.findViewById(R.id.chatRecyclerView)
        chatRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        chatAdapter = ChatAdapter(requireContext(), this)
        chatRecyclerView.adapter = chatAdapter

        // Initialize the chat item list
        chatItemList = mutableListOf()
        // Set the chat items in the adapter
        chatAdapter.setChatItems(chatItemList)

        retrieveChatItems()
    }

    // Inside your retrieveChatItems function
    // Inside your retrieveChatItems function
    // Inside your retrieveChatItems function
    private fun retrieveChatItems() {
        chatItemList.clear()

        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        friendsRef = currentUserUid?.let {
            FirebaseDatabase.getInstance().getReference("users")
                .child(it).child("friends")
        }!!

        friendsListener = friendsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                chatItemList.clear()
                for (friendSnapshot in snapshot.children) {
                    val friendUid = friendSnapshot.getValue(String::class.java)

                    val userRef = FirebaseDatabase.getInstance().getReference("users")
                        .child(friendUid!!)
                    userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        @SuppressLint("NotifyDataSetChanged")
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val friend = snapshot.getValue(User::class.java)
                            if (friend != null) {
                                val chatItem = friend.userName?.let {
                                    ChatItem(
                                        it,
                                        "",
                                        "",
                                        friend.imageUrl
                                    )
                                }
                                if (chatItem != null) {
                                    chatItem.userUid = friendUid
                                }

                                // Use addChildEventListener for real-time updates
                                getLastMessageAndTimestamp(currentUserUid, friendUid) { lastMessage, lastTimestamp ->
                                    chatItem?.lastMessage = lastMessage
                                    chatItem?.lastTimestamp = lastTimestamp
                                    chatAdapter.notifyDataSetChanged()
                                }

                                if (chatItem != null) {
                                    chatItemList.add(chatItem)
                                    chatAdapter.notifyDataSetChanged()
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Handle error
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun getLastMessageAndTimestamp(currentUserUid: String, friendUid: String, onComplete: (String, String) -> Unit) {
        val chatRef = FirebaseDatabase.getInstance().getReference("chat")
        val currentUserFriendRef = chatRef.child(currentUserUid).child(friendUid)
        val friendUserFriendRef = chatRef.child(friendUid).child(currentUserUid)

        val lastMessageRef = currentUserFriendRef.child("messages").orderByChild("timestamp").limitToLast(1)
        lastMessageRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java)
                if (message != null) {
                    val lastMessage = message.message.toString()
                    val lastTimestamp = message.timestamp.toString()
                    onComplete(lastMessage, lastTimestamp)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle change in child if needed
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // Handle removed child if needed
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle moved child if needed
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })

        val friendLastMessageRef = friendUserFriendRef.child("messages").orderByChild("timestamp").limitToLast(1)
        friendLastMessageRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java)
                if (message != null) {
                    val lastMessage = message.message.toString()
                    val lastTimestamp = message.timestamp.toString()
                    onComplete(lastMessage, lastTimestamp)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle change in child if needed
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // Handle removed child if needed
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle moved child if needed
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }




    override fun onDestroyView() {
        super.onDestroyView()
        // Unregister the ValueEventListener
        friendsRef.removeEventListener(friendsListener)
    }

    override fun onChatItemClick(position: Int) {
        // Handle chat item click
        val chatItem = chatItemList[position]

        val intent = Intent(requireContext(), ChatActivity::class.java)
        intent.putExtra("friendUid", chatItem.userUid)
        intent.putExtra("friendUserName", chatItem.userName)
        intent.putExtra("userImage", chatItem.userImageRes)
        startActivity(intent)
    }
}