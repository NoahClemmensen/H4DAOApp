package com.h4.dao

import com.google.gson.annotations.SerializedName
import com.h4.dao.interfaces.IShop

data class PendingPackage(
    val barcode: Int,
    @SerializedName("shop_address") override val shopAddress: String,
    @SerializedName("shop_email") override val shopEmail: String,
    @SerializedName("shop_name") override val shopName: String,
    @SerializedName("shop_phone") override val shopPhone: String,
    @SerializedName("shop_zipcode") override val shopZipcode: String
): IShop
