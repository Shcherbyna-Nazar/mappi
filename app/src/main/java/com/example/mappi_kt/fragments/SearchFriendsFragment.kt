package com.example.mappi_kt.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mappi_kt.HomeActivity
import com.example.mappi_kt.R
import com.example.mappi_kt.adapters.FriendAdapter
import com.example.mappi_kt.entity.FriendRequest
import com.example.mappi_kt.entity.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SearchFriendsFragment : Fragment(), FriendAdapter.OnItemClickListener, FriendAdapter.OnAddFriendClickListener {
    private lateinit var searchEditText: EditText
    private lateinit var friendsRecyclerView: RecyclerView
    private lateinit var friendAdapter: FriendAdapter
    private lateinit var friendsList: List<User>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.friends_add, container, false)

        // Initialize views
        searchEditText = view.findViewById(R.id.searchFriendEditText)
        friendsRecyclerView = view.findViewById(R.id.friendsRecyclerView)

        // Set up RecyclerView
        friendAdapter = FriendAdapter()
        friendAdapter.setOnItemClickListener(this)
        friendAdapter.setOnAddFriendClickListener(this)

        friendsRecyclerView.layoutManager = LinearLayoutManager(activity)
        friendsRecyclerView.adapter = friendAdapter

        getFriendsList(object : FriendsListCallback {
            override fun onFriendsListRetrieved(userList: List<User>) {
                friendsList = userList
                friendAdapter.setUsers(friendsList)
            }

            override fun onFriendsListError(errorMessage: String) {
                // Handle the error, e.g., show an error message
            }
        })

        // Set up search functionality
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                val searchQuery = s.toString().trim()
                performSearch(searchQuery)
            }
        })

        return view
    }

    private fun performSearch(searchQuery: String) {
        val filteredFriends = ArrayList<User>()

        if (searchQuery.isEmpty()) {
            // If the search query is empty, show all friends
            filteredFriends.addAll(friendsList)
        } else {
            // Perform the search based on the search query
            for (friend in friendsList) {
                if (friend.userName?.toLowerCase()
                        ?.contains(searchQuery.toLowerCase())!!) {
                    filteredFriends.add(friend)
                }
            }
        }

        friendAdapter.setUsers(filteredFriends)
    }

    private fun getFriendsList(callback: FriendsListCallback) {
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference("users")

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val userList = ArrayList<User>()
                for (userSnapshot in dataSnapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    // Exclude the currently logged-in user from the friends list
                    if (!user!!.email.equals(FirebaseAuth.getInstance().currentUser?.email, ignoreCase = true)) {
                        userList.add(user)
                    }
                }
                callback.onFriendsListRetrieved(userList)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                callback.onFriendsListError(databaseError.message)
            }
        })
    }

    override fun onItemClick(user: User) {
        openProfileFragment(user)
    }

    override fun onAddFriendClick(user: User) {
        // Check if the users are already friends
        val currentUserUid = FirebaseAuth.getInstance().currentUser!!.uid
        val friendUid = user.userId

        val friendRequestsRef = FirebaseDatabase.getInstance().getReference("friendRequests")

        friendRequestsRef.orderByChild("senderUid").equalTo(currentUserUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (requestSnapshot in snapshot.children) {
                        val friendRequest = requestSnapshot.getValue(FriendRequest::class.java)
                        if (friendRequest != null && friendRequest.receiverUid == friendUid) {
                            // Friend request already sent, delete it
                            friendRequest.requestId?.let { deleteFriendRequest(it) }
                            return
                        }
                    }

                    // Friend request not sent, send a new friend request
                    sendFriendRequest(user)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle the error, e.g., show an error message
                }
            })
    }

    private fun deleteFriendRequest(requestId: String) {
        val friendRequestsRef = FirebaseDatabase.getInstance().getReference("friendRequests")

        friendRequestsRef.child(requestId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    snapshot.ref.removeValue()
                        .addOnSuccessListener {
                            // Friend request deleted successfully
                            Toast.makeText(activity, "Friend request canceled", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            // Error occurred while deleting friend request
                            // You can handle the failure case as needed
                        }
                }  // Handle the case where the friend request no longer exists
                // or has already been deleted
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle the error, e.g., show an error message
            }
        })
    }

    private fun sendFriendRequest(user: User) {
        val currentUserUid = FirebaseAuth.getInstance().currentUser!!.uid
        val friendUid = user.userId

        val friendRequestsRef = FirebaseDatabase.getInstance().getReference("friendRequests")

        // Generate a unique request ID
        val requestId = "${currentUserUid}_$friendUid"

        val friendRequest = FriendRequest(currentUserUid, friendUid)
        friendRequestsRef.child(requestId).setValue(friendRequest)
            .addOnSuccessListener {
                FriendRequestsFragment.friendRequestsList.add(friendRequest)
                // Friend request sent successfully
                Toast.makeText(activity, "Friend request was sent successfully to ${user.userName}", Toast.LENGTH_SHORT).show()
                // You can handle the success case as needed
            }
            .addOnFailureListener { e ->
                // Error occurred while sending friend request
                // You can handle the failure case as needed
            }
    }

    private fun openProfileFragment(user: User) {
        val intent = Intent(activity, HomeActivity::class.java)
        intent.putExtra("userId", user.userId)
        intent.putExtra("openProfileFragment", true)
        startActivity(intent)
        activity?.finish()
    }

    interface FriendsListCallback {
        fun onFriendsListRetrieved(userList: List<User>)
        fun onFriendsListError(errorMessage: String)
    }
}
