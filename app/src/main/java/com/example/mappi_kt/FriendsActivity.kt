package com.example.mappi_kt

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.mappi_kt.entity.User
import com.example.mappi_kt.ui.RoundedCornerImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FriendsActivity : AppCompatActivity() {

    private lateinit var searchFriendEditText: EditText
    private lateinit var friendsRecyclerView: RecyclerView
    private lateinit var searchResultsTextView: TextView

    private lateinit var usersRef: DatabaseReference
    private lateinit var friendsList: List<User>
    private lateinit var friendsAdapter: FriendsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.friends_activity)

        searchFriendEditText = findViewById(R.id.searchFriendEditText)
        friendsRecyclerView = findViewById(R.id.friendsRecyclerView)
        searchResultsTextView = findViewById(R.id.searchResultsTextView)

        friendsAdapter = FriendsAdapter()
        friendsRecyclerView.layoutManager = LinearLayoutManager(this)
        friendsRecyclerView.adapter = friendsAdapter

        // Get a reference to the "users" node in the Firebase Realtime Database
        usersRef = FirebaseDatabase.getInstance().getReference("users")

        // Retrieve friends from Firebase
        retrieveFriendsFromFirebase(object : FriendsListCallback {
            override fun onFriendsListRetrieved(userList: List<User>) {
                friendsList = userList
                friendsAdapter.setFriends(friendsList)
            }

            override fun onFriendsListError(errorMessage: String) {
                // Handle error retrieving friends
                Toast.makeText(this@FriendsActivity, errorMessage, Toast.LENGTH_SHORT).show()
            }
        })

        searchFriendEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val searchQuery = s.toString().trim()
                performSearch(searchQuery)
            }
        })
    }

    private fun retrieveFriendsFromFirebase(callback: FriendsListCallback) {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        val friendsRef = usersRef.child(currentUserUid!!).child("friends")
        friendsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val friendsList = mutableListOf<User>()

                for (userSnapshot in snapshot.children) {
                    val friendUid = userSnapshot.getValue(String::class.java)
                    usersRef.child(friendUid!!)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val user = snapshot.getValue(User::class.java)
                                if (user != null) {
                                    friendsList.add(user)
                                }
                                callback.onFriendsListRetrieved(friendsList)
                            }

                            override fun onCancelled(error: DatabaseError) {
                                callback.onFriendsListError("Failed to retrieve friend: " + error.message)
                            }
                        })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                callback.onFriendsListError("Failed to retrieve friends: " + error.message)
            }
        })
    }

    private fun performSearch(searchQuery: String) {
        val filteredFriends = mutableListOf<User>()

        if (searchQuery.isEmpty()) {
            // If the search query is empty, show all friends
            filteredFriends.addAll(friendsList)
        } else {
            // Perform the search based on the search query
            for (friend in friendsList) {
                if (friend.userName?.toLowerCase()?.contains(searchQuery.toLowerCase())!!) {
                    filteredFriends.add(friend)
                }
            }
        }
        friendsAdapter.setFriends(filteredFriends)
    }

    inner class FriendsAdapter : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {

        private var friendsList = mutableListOf<User>()

        @SuppressLint("NotifyDataSetChanged")
        fun setFriends(friends: List<User>) {
            friendsList.clear()
            friendsList.addAll(friends)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_friend, parent, false)
            return FriendViewHolder(view)
        }

        override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
            val friend = friendsList[position]
            holder.friendName.text = friend.userName

            // Set the appropriate image for the friend using the friend's image URL
            // You can use the Glide library to load and display the images
            Glide.with(holder.itemView.context)
                .load(friend.imageUrl)
                .placeholder(R.drawable.ic_person_foreground) // Placeholder image while loading
                .error(R.drawable.ic_person_foreground) // Error image if loading fails
                .transition(DrawableTransitionOptions.withCrossFade()) // Fade-in animation
                .into(holder.friendIcon)

            holder.deleteFriendIcon.setOnClickListener {
                // Handle delete friend icon click and perform the corresponding action
                // For example, you can delete the friend from the Firebase Realtime Database
                val friendUid = friend.userId
                friendUid?.let { it1 -> deleteFriendFromDatabase(it1) }
            }
        }

        private fun deleteFriendFromDatabase(friendUid: String) {
            val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
            val friendsRef = usersRef.child(currentUserUid!!).child("friends")

            friendsRef.orderByValue().equalTo(friendUid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        for (friendSnapshot in dataSnapshot.children) {
                            val friendUid = friendSnapshot.getValue(String::class.java)

                            friendSnapshot.ref.removeValue()
                                .addOnSuccessListener {
                                    var position = -1
                                    for ((index, friend) in friendsList.withIndex()) {
                                        position = index
                                        if (friend.userId == friendUid) {
                                            break
                                        }
                                    }
                                    if (position != -1) {
                                        friendsList.removeAt(position)
                                        friendsAdapter.notifyItemRemoved(position)
                                    }
                                    // Friend deletion successful
                                    Toast.makeText(
                                        this@FriendsActivity,
                                        "Friend deleted",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnFailureListener {
                                    // Friend deletion failed
                                    Toast.makeText(
                                        this@FriendsActivity,
                                        "Failed to delete friend",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Handle the error, e.g., show an error message
                        Toast.makeText(
                            this@FriendsActivity,
                            "Failed to delete friend: " + databaseError.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })

            val friendFriendsRef = usersRef.child(friendUid).child("friends")

            friendFriendsRef.orderByValue().equalTo(currentUserUid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        for (friendSnapshot in dataSnapshot.children) {
                            friendSnapshot.ref.removeValue()
                                .addOnSuccessListener {
                                    // Friend deletion successful
                                    // Toast.makeText(com.example.mappi_kt.FriendsActivity.this, "Friend deleted", Toast.LENGTH_SHORT).show();
                                }
                                .addOnFailureListener {
                                    // Friend deletion failed
                                    Toast.makeText(
                                        this@FriendsActivity,
                                        "Failed to delete friend",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Handle the error, e.g., show an error message
                        Toast.makeText(
                            this@FriendsActivity,
                            "Failed to delete friend: " + databaseError.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }

        override fun getItemCount(): Int {
            return friendsList.size
        }

        inner class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val friendIcon: RoundedCornerImageView = itemView.findViewById(R.id.friendIcon)
            val friendName: TextView = itemView.findViewById(R.id.friendName)
            val deleteFriendIcon: ImageView = itemView.findViewById(R.id.deleteFriendIcon)
        }
    }

    interface FriendsListCallback {
        fun onFriendsListRetrieved(userList: List<User>)

        fun onFriendsListError(errorMessage: String)
    }
}
