package com.example.havadurumu

import com.google.firebase.Timestamp

/** Firestore "posts" koleksiyonundaki bir gökyüzü/hava durumu paylaşımı. */
data class Post(
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val cityName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val tempC: Double = 0.0,
    val conditionText: String = "",
    val conditionIcon: String = "",
    val imageBase64: String = "",
    val createdAt: Timestamp? = null
)
