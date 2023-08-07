package com.example.mappi_kt.entity

class Message {
    var message: String? = null
    var timestamp: String? = null
    var date: String? = null
    var sent: Boolean = false
    var senderUid: String? = null
    var receiverUid: String? = null

    constructor() {
        // Default constructor required for Firebase Realtime Database
    }

    constructor(
        message: String?,
        timestamp: String?,
        date: String?,
        sent: Boolean,
        senderUid: String?,
        receiverUid: String?
    ) {
        this.message = message
        this.timestamp = timestamp
        this.sent = sent
        this.date = date
        this.senderUid = senderUid
        this.receiverUid = receiverUid
    }
}
