package com.byte2bites.delivery

data class User(
    val fullName: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    var photoUrl: String? = null
)