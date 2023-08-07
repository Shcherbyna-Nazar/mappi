package com.example.mappi_kt.entity

data class Post(
    var urlImage: String = "",
    var location: MyLocation = MyLocation(),
    var timestamp: String = ""
) {
    constructor() : this("", MyLocation(), "")
}
