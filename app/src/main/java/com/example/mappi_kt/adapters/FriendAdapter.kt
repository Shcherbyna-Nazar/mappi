package com.example.mappi_kt.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.mappi_kt.R
import com.example.mappi_kt.entity.FriendRequest
import com.example.mappi_kt.entity.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class FriendAdapter : RecyclerView.Adapter<FriendAdapter.FriendViewHolder>() {
    private var users: List<User> = ArrayList()
    private val friendRequests: MutableMap<String, FriendRequest> = HashMap()
    private var itemClickListener: OnItemClickListener? = null
    private var onAddFriendClickListener: OnAddFriendClickListener? = null

    init {
        retrieveFriendRequests()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setUsers(users: List<User>) {
        this.users = users
        notifyDataSetChanged()
    }

    private fun retrieveFriendRequests() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser!!.uid
        val friendRequestsRef = FirebaseDatabase.getInstance().getReference("friendRequests")

        friendRequestsRef.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                friendRequests.clear()
                for (dataSnapshot in snapshot.children) {
                    val friendRequest = dataSnapshot.getValue(FriendRequest::class.java)
                    if (friendRequest != null) {
                        if (friendRequest.senderUid == currentUserUid) {
                            friendRequests[friendRequest.receiverUid!!] = friendRequest

                        } else if (friendRequest.receiverUid == currentUserUid) {
                            friendRequests[friendRequest.senderUid!!] = friendRequest
                        }
                    }
                }
                notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle the error, e.g., show an error message
            }
        })
    }

    fun setOnItemClickListener(listener: OnItemClickListener?) {
        itemClickListener = listener
    }

    fun setOnAddFriendClickListener(listener: OnAddFriendClickListener?) {
        onAddFriendClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return FriendViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val user = users[position]
        holder.bind(user)
        holder.updateAddFriendIcon(user)
    }

    override fun getItemCount(): Int {
        return users.size
    }

    interface OnItemClickListener {
        fun onItemClick(user: User)
    }

    interface OnAddFriendClickListener {
        fun onAddFriendClick(user: User)
    }

    inner class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        private val friendIcon: ImageView = itemView.findViewById(R.id.friendIcon)
        private val friendName: TextView = itemView.findViewById(R.id.friendName)
        private val addFriendIcon: ImageView = itemView.findViewById(R.id.addFriendIcon)

        init {
            itemView.setOnClickListener(this)
            addFriendIcon.setOnClickListener(this)
        }

        fun bind(user: User) {
            friendIcon.setImageDrawable(null)
            if (user.imageUrl.isNotEmpty()) {
                Glide.with(itemView)
                    .load(user.imageUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.ic_person_foreground)
                    .error(R.drawable.ic_person_foreground)
                    .into(friendIcon)
            } else {
                friendIcon.setImageResource(R.drawable.ic_person_foreground)
            }
            friendName.text = user.userName
        }

        fun updateAddFriendIcon(user: User) {
            val userId = user.userId
            if (friendRequests.containsKey(userId)) {
                addFriendIcon.setImageResource(R.drawable.sent_request_foreground)
                addFriendIcon.isEnabled = true
                return
            }
            var isAccepted = false
            val currentUserUid = FirebaseAuth.getInstance().currentUser!!.uid
            val usersRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("users")
            val friendsRef: DatabaseReference = usersRef.child(currentUserUid).child("friends")

            friendsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    isAccepted = false
                    for (friend in snapshot.children) {
                        val friendUid = friend.getValue(String::class.java)
                        if (friendUid == user.userId) {
                            isAccepted = true
                            break
                        }
                    }
                    updateAddFriendIconState(isAccepted)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle the error, e.g., show an error message
                }
            })

            updateAddFriendIconState(isAccepted)
        }

        fun updateAddFriendIconState(isAccepted: Boolean) {
            if (isAccepted) {
                addFriendIcon.setImageResource(R.drawable.accept_request_foreground)
                addFriendIcon.isEnabled = false
            } else {
                addFriendIcon.setImageResource(R.drawable.add_user_icon_foreground)
                addFriendIcon.isEnabled = true
            }
        }

        override fun onClick(v: View) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val user = users[position]
                if (v.id == R.id.addFriendIcon && onAddFriendClickListener != null) {
                    onAddFriendClickListener?.onAddFriendClick(user)
                } else if (itemClickListener != null) {
                    itemClickListener?.onItemClick(user)
                }
            }
        }
    }
}
