package com.smartlist.budgetcalc

import java.util.UUID

data class ShopItem(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var price: Double,
    var isBought: Boolean = false
)
