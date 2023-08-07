package com.example.mappi_kt.entity

class FriendRequest {
    var requestId: String? = null
    var senderUid: String? = null
    var receiverUid: String? = null

    constructor() {
        // Required empty constructor for Firebase
    }

    constructor(senderUid: String?, receiverUid: String?) {
        this.senderUid = senderUid
        this.receiverUid = receiverUid
        this.requestId = generateRequestId()
    }

    private fun generateRequestId(): String {
        return "${senderUid}_$receiverUid"
    }
}
