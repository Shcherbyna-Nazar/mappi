package com.example.mappi_kt.entity

class ChatItem(
    val userName: String,
    var lastMessage: String,
    var lastTimestamp: String,
    val userImageRes: String
) {
    var userUid: String? = null
}
