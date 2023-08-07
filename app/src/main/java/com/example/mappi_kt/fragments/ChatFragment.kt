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
                                if (chatItem != null) {
                                    chatItemList.add(chatItem)
                                }

                                val lastMessage = getLastMessage(friendUid)
                                val lastTimestamp = getLastTimestamp(friendUid)
                                updateChatItemLastMessageAndTimestamp(friendUid, lastMessage, lastTimestamp)

                                chatAdapter.notifyDataSetChanged()
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

    private fun getLastMessage(friendUid: String): String {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        val chatRef = FirebaseDatabase.getInstance().getReference("chat")
        val currentUserFriendRef = chatRef.child(currentUserUid!!).child(friendUid)
        val friendUserFriendRef = chatRef.child(friendUid).child(currentUserUid)

        val lastMessageRef = currentUserFriendRef.child("messages").orderByChild("timestamp").limitToLast(1)
        val friendLastMessageRef = friendUserFriendRef.child("messages").orderByChild("timestamp").limitToLast(1)

        var lastMessage = ""

        lastMessageRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (messageSnapshot in snapshot.children) {
                    val message = messageSnapshot.getValue(Message::class.java)
                    if (message != null) {
                        lastMessage = message.message.toString()
                    }
                }
                // Update the chat item list with the last message
                updateChatItemLastMessage(friendUid, lastMessage)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })

        friendLastMessageRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (messageSnapshot in snapshot.children) {
                    val message = messageSnapshot.getValue(Message::class.java)
                    if (message != null) {
                        lastMessage = message.message.toString()
                    }
                }
                // Update the chat item list with the last message
                updateChatItemLastMessage(friendUid, lastMessage)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })

        return lastMessage
    }

    private fun getLastTimestamp(friendUid: String): String {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        val chatRef = FirebaseDatabase.getInstance().getReference("chat")
        val currentUserFriendRef = chatRef.child(currentUserUid!!).child(friendUid)
        val friendUserFriendRef = chatRef.child(friendUid).child(currentUserUid)

        val lastTimestampRef = currentUserFriendRef.child("messages").orderByChild("timestamp").limitToLast(1)
        val friendLastTimestampRef = friendUserFriendRef.child("messages").orderByChild("timestamp").limitToLast(1)

        var lastTimestamp = ""

        lastTimestampRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (messageSnapshot in snapshot.children) {
                    val message = messageSnapshot.getValue(Message::class.java)
                    if (message != null) {
                        lastTimestamp = message.timestamp.toString()
                    }
                }
                // Update the chat item list with the last timestamp
                updateChatItemLastTimestamp(friendUid, lastTimestamp)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })

        friendLastTimestampRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (messageSnapshot in snapshot.children) {
                    val message = messageSnapshot.getValue(Message::class.java)
                    if (message != null) {
                        lastTimestamp = message.timestamp.toString()
                    }
                }
                // Update the chat item list with the last timestamp
                updateChatItemLastTimestamp(friendUid, lastTimestamp)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })

        return lastTimestamp
    }

    private fun updateChatItemLastMessage(friendUid: String, lastMessage: String) {
        for (chatItem in chatItemList) {
            if (chatItem.userUid == friendUid) {
                chatItem.lastMessage = lastMessage
                break
            }
        }
    }

    private fun updateChatItemLastTimestamp(friendUid: String, lastTimestamp: String) {
        for (chatItem in chatItemList) {
            if (chatItem.userUid == friendUid) {
                chatItem.lastTimestamp = lastTimestamp
                break
            }
        }
    }

    private fun updateChatItemLastMessageAndTimestamp(friendUid: String, lastMessage: String, lastTimestamp: String) {
        for (chatItem in chatItemList) {
            if (chatItem.userUid == friendUid) {
                chatItem.lastMessage = lastMessage
                chatItem.lastTimestamp = lastTimestamp
                break
            }
        }
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