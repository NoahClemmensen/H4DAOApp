package com.h4.dao

import com.google.gson.annotations.SerializedName
import com.h4.dao.interfaces.ISender
import com.h4.dao.interfaces.IShop

data class Delivery(
    val barcode: Int,
    @SerializedName("created_date") val createdDate: String,
    @SerializedName("delivery_time") val deliveryTime: String?,
    @SerializedName("delivery_status") val deliveryStatus: String,
    @SerializedName("sender_name") override val senderName: String,
    @SerializedName("shop_name") override val shopName: String,
    @SerializedName("sender_address") override val senderAddress: String,
    @SerializedName("shop_address") override val shopAddress: String,
    @SerializedName("sender_zip_code") override val senderZipcode: String,
    @SerializedName("shop_zip_code") override val shopZipcode: String,
    @SerializedName("sender_phone") override val senderPhone: String,
    @SerializedName("shop_phone") override val shopPhone: String,
    @SerializedName("sender_email") override val senderEmail: String,
    @SerializedName("shop_email") override val shopEmail: String,
): IShop, ISender
