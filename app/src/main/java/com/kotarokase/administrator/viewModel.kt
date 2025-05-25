package com.kotarokase.administrator

import androidx.lifecycle.ViewModel

class ItemViewModel : ViewModel() {
    companion object {
        var selectedItem: OrderItem? = null
    }
}