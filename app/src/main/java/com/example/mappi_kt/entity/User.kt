package com.example.mappi_kt.entity

class User {
    var userId: String? = null
    var userName: String? = null
    var email: String? = null
    var imageUrl: String = ""
    var phoneNumber: Int = 0
    var friends: MutableList<String> = mutableListOf()

    constructor(userName: String, email: String) {
        this.userName = userName
        this.email = email
    }

    constructor() {
    }

    constructor(
        userId: String,
        userName: String,
        email: String,
        imageUrl: String,
        phoneNumber: Int
    ) {
        this.userId = userId
        this.userName = userName
        this.email = email
        this.imageUrl = imageUrl
        this.phoneNumber = phoneNumber
    }

    fun addFriend(friendUid: String) {
        if (!friends.contains(friendUid)) {
            friends.add(friendUid)
        }
    }
}
