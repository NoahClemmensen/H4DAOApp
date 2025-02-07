package com.h4.dao

data class Package(
    val id: Int,
    val shopId: Int,
    val senderId: Int,
    val createdDate: Int,
    val deliveryTime: Int?,
    val deliveryStatus: String
)
