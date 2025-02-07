package com.h4.dao

import com.google.gson.annotations.SerializedName
import com.h4.dao.interfaces.IShop

data class PendingPackage(
    val barcode: Int,
    @SerializedName("shop_address") override val address: String,
    @SerializedName("shop_email") override val email: String,
    @SerializedName("shop_name") override val name: String,
    @SerializedName("shop_phone") override val phone: String,
    @SerializedName("shop_zipcode") override val zipcode: String
): IShop
