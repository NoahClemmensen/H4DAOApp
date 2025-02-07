package com.h4.dao

import com.h4.dao.interfaces.IShop

data class PendingPackage(
    val barcode: Int,
    override val address: String,
    override val email: String,
    override val name: String,
    override val phone: String,
    override val zipcode: String
): IShop
