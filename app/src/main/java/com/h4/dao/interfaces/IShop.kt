package com.h4.dao.interfaces

import com.google.gson.annotations.SerializedName

interface IShop {
    val shopName: String;
    val shopAddress: String;
    val shopZipcode: String;
    val shopPhone: String;
    val shopEmail: String;
}