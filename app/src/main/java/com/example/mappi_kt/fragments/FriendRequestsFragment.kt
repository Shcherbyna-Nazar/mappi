package com.example.mappi_kt.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mappi_kt.adapters.FriendRequestAdapter
import com.example.mappi_kt.R
import com.example.mappi_kt.entity.FriendRequest
import com.example.mappi_kt.entity.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class FriendRequestsFragment : Fragment() {
    private lateinit var friendRequestsRecyclerView: RecyclerView

    companion object {
        @JvmStatic
        var friendRequestsList: MutableList<FriendRequest> = ArrayList()
    }

    private lateinit var friendRequestsRef: DatabaseReference
    private lateinit var currentUserUid: String
    private lateinit var friendRequestAdapter: FriendRequestAdapter
    private lateinit var friendRequestsListener: ChildEventListener


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.friend_request, container, false)

        currentUserUid = FirebaseAuth.getInstance().currentUser!!.uid
        friendRequestsRef = FirebaseDatabase.getInstance().getReference("friendRequests")

        friendRequestsRecyclerView = view.findViewById(R.id.friendRequestsRecyclerView)
        friendRequestsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        friendRequestAdapter = FriendRequestAdapter()
        setupFriendRequestsListener()

        friendRequestAdapter.setAcceptButtonClickListener(object :
            FriendRequestAdapter.OnAcceptButtonClickListener {
            override fun onAcceptButtonClick(friendRequest: FriendRequest?) {
                val senderUid = friendRequest?.senderUid
                val usersRef = FirebaseDatabase.getInstance().getReference("users")
                senderUid?.let {
                    usersRef.child(it).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists()) {
                                val senderUser = dataSnapshot.getValue(User::class.java)
                                if (senderUser != null) {
                                    val currentUserRef =
                                        usersRef.child(FirebaseAuth.getInstance().currentUser!!.uid)
                                    currentUserRef.addListenerForSingleValueEvent(object :
                                        ValueEventListener {
                                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                                            if (dataSnapshot.exists()) {
                                                val currentUser =
                                                    dataSnapshot.getValue(User::class.java)
                                                if (currentUser != null) {
                                                    currentUser.addFriend(senderUid)
                                                    senderUser.addFriend(currentUserUid)
                                                    currentUserRef.setValue(currentUser)
                                                    usersRef.child(senderUid).setValue(senderUser)
                                                }
                                            }
                                        }

                                        override fun onCancelled(databaseError: DatabaseError) {
                                            Toast.makeText(
                                                requireContext(),
                                                "Failed to add friend: " + databaseError.message,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    })
                                    friendRequestsRef.child(
                                        generateRequestId(
                                            senderUid,
                                            currentUserUid
                                        )
                                    )
                                        .removeValue()
                                        .addOnSuccessListener {
                                            friendRequestsList.remove(friendRequest)
                                            friendRequestAdapter.notifyDataSetChanged()
                                            Toast.makeText(
                                                requireContext(),
                                                "Friend added successfully",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(
                                                requireContext(),
                                                "Failed to add friend: " + e.message,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            // Handle the error, e.g., show an error message
                        }
                    })
                }
            }
        })

        friendRequestAdapter.setDeclineButtonClickListener(object :
            FriendRequestAdapter.OnDeclineButtonClickListener {
            override fun onDeclineButtonClick(friendRequest: FriendRequest?) {
                val senderUid = friendRequest?.senderUid
                senderUid?.let { generateRequestId(it, currentUserUid) }?.let {
                    friendRequestsRef.child(it)
                        .removeValue()
                        .addOnSuccessListener {
                            friendRequestsList.remove(friendRequest)
                            friendRequestAdapter.notifyDataSetChanged()
                            Toast.makeText(
                                requireContext(),
                                "Friend request declined",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                requireContext(),
                                "Failed to decline friend request: " + e.message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
        })

        friendRequestsRecyclerView.adapter = friendRequestAdapter

        getFriendRequestsList(object : FriendRequestsListCallback {
            override fun onFriendRequestsListRetrieved(friendRequests: List<FriendRequest>) {
                friendRequestsList = friendRequests as MutableList<FriendRequest>
                friendRequestAdapter.setFriendRequests(friendRequestsList)
            }

            override fun onFriendRequestsListError(errorMessage: String) {
                // Handle the error, e.g., show an error message
            }
        })

        return view
    }

    private fun setupFriendRequestsListener() {
        friendRequestsListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                val friendRequest = dataSnapshot.getValue(FriendRequest::class.java)
                if (friendRequest != null) {
                    friendRequestsList.add(friendRequest)
                    friendRequestAdapter.notifyDataSetChanged()
                }
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
                val friendRequest = dataSnapshot.getValue(FriendRequest::class.java)
                if (friendRequest != null) {
                    val index = friendRequest.requestId?.let { findFriendRequestIndex(it) }
                    if (index != -1) {
                        friendRequestsList[index!!] = friendRequest
                        friendRequestAdapter.notifyDataSetChanged()
                    }
                }
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                val friendRequest = dataSnapshot.getValue(FriendRequest::class.java)
                if (friendRequest != null) {
                    val index = friendRequest.requestId?.let { findFriendRequestIndex(it) }
                    if (index != -1) {
                        if (index != null) {
                            friendRequestsList.removeAt(index)
                        }
                        index?.let { friendRequestAdapter.notifyItemRemoved(it) }
                    }
                }
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {
                // Handle moved friend requests, if necessary
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle the error, e.g., show an error message
            }
        }

        friendRequestsRef.orderByChild("receiverUid").equalTo(currentUserUid)
            .addChildEventListener(friendRequestsListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (friendRequestsListener != null) {
            friendRequestsRef.removeEventListener(friendRequestsListener)
        }
    }

    private fun findFriendRequestIndex(requestId: String): Int {
        for (i in friendRequestsList.indices) {
            val friendRequest = friendRequestsList[i]
            if (friendRequest.requestId == requestId) {
                return i
            }
        }
        return -1
    }

    private fun getFriendRequestsList(callback: FriendRequestsListCallback) {
        val friendRequestsRef = FirebaseDatabase.getInstance().getReference("friendRequests")

        friendRequestsRef.orderByChild("receiverUid")
            .equalTo(FirebaseAuth.getInstance().currentUser!!.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val friendRequests: MutableList<FriendRequest> = ArrayList()
                    for (snapshot in dataSnapshot.children) {
                        val friendRequest = snapshot.getValue(FriendRequest::class.java)
                        if (friendRequest != null) {
                            friendRequests.add(friendRequest)
                        }
                    }
                    callback.onFriendRequestsListRetrieved(friendRequests)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    callback.onFriendRequestsListError(databaseError.message)
                }
            })
    }

    private fun generateRequestId(senderUid: String, receiverUid: String): String {
        return "${senderUid}_$receiverUid"
    }

    interface FriendRequestsListCallback {
        fun onFriendRequestsListRetrieved(friendRequests: List<FriendRequest>)
        fun onFriendRequestsListError(errorMessage: String)
    }
}
