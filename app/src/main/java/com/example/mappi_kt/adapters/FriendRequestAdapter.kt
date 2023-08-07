package com.example.mappi_kt.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.example.mappi_kt.R
import com.example.mappi_kt.entity.FriendRequest
import com.example.mappi_kt.entity.User
import com.google.firebase.database.*

class FriendRequestAdapter : RecyclerView.Adapter<FriendRequestAdapter.ViewHolder>() {

    private var friendRequests: List<FriendRequest>? = null
    private var acceptButtonClickListener: OnAcceptButtonClickListener? = null
    private var declineButtonClickListener: OnDeclineButtonClickListener? = null

    fun setFriendRequests(friendRequests: List<FriendRequest>?) {
        this.friendRequests = friendRequests
        notifyDataSetChanged()
    }

    fun setAcceptButtonClickListener(listener: OnAcceptButtonClickListener?) {
        acceptButtonClickListener = listener
    }

    fun setDeclineButtonClickListener(listener: OnDeclineButtonClickListener?) {
        declineButtonClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend_request, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val friendRequest = friendRequests?.get(position)
        val senderUid = friendRequest?.senderUid

        if (!senderUid.isNullOrEmpty()) {
            val usersRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("users")
            usersRef.child(senderUid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val user = dataSnapshot.getValue(User::class.java)

                        holder.userNameTextView.text = user?.userName
                        val requestOptions = RequestOptions().placeholder(R.drawable.ic_person_foreground).error(R.drawable.ic_person_foreground)

                        Glide.with(holder.itemView.context)
                            .load(user?.imageUrl)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .apply(requestOptions)
                            .into(holder.profileImageView)

                        holder.acceptButton.setOnClickListener {
                            acceptButtonClickListener?.onAcceptButtonClick(friendRequest)
                        }

                        holder.declineButton.setOnClickListener {
                            declineButtonClickListener?.onDeclineButtonClick(friendRequest)
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle the error, e.g., show an error message
                }
            })
        } else {
            holder.userNameTextView.text = "Unknown User"
            holder.profileImageView.setImageResource(R.drawable.ic_person_foreground)
            holder.profileImageView.setBackgroundResource(R.drawable.image_corners_back)
        }
    }

    override fun getItemCount(): Int {
        return friendRequests?.size ?: 0
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImageView: ImageView = itemView.findViewById(R.id.profileImageView)
        val userNameTextView: TextView = itemView.findViewById(R.id.userNameTextView)
        val acceptButton: Button = itemView.findViewById(R.id.acceptButton)
        val declineButton: Button = itemView.findViewById(R.id.declineButton)
    }

    interface OnAcceptButtonClickListener {
        fun onAcceptButtonClick(friendRequest: FriendRequest?)
    }

    interface OnDeclineButtonClickListener {
        fun onDeclineButtonClick(friendRequest: FriendRequest?)
    }
}
