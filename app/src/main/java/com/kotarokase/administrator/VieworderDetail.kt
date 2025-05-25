package com.kotarokase.administrator

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

@Composable
fun DetailedOrderView() {
    val selectedItem = ItemViewModel.selectedItem // You set this elsewhere
    var selectedStatus by remember { mutableStateOf(selectedItem?.status ?: "") }
    val statusOptions = listOf("picked up", "shipped", "completed", "cancelled")
    var expanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    if (selectedItem == null) {
        Text("No order selected")
        return
    }

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)) {
        Text("Order ID: ${selectedItem.orderId}", style = MaterialTheme.typography.titleLarge)
        Text("User ID: ${selectedItem.userId}")
        Text("Item ID: ${selectedItem.itemId}")
        Text("Quantity: ${selectedItem.quantity}")
        Text("Date: ${selectedItem.orderDate}")

        Spacer(modifier = Modifier.height(20.dp))

        Text("Update Status", style = MaterialTheme.typography.titleMedium)

            Box(modifier = Modifier.fillMaxWidth()) {
                Button(onClick = {expanded = true}) {
                    Text(selectedStatus)
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    statusOptions.forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status) },
                            onClick = {
                                selectedStatus = status
                                expanded = false
                            }
                        )
                    }
                }
            }


            Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                isLoading = true
                updateOrderStatus(
                    userId = selectedItem.userId,
                    orderDate = selectedItem.orderDate,
                    newStatus = selectedStatus,
                    onResult = { isLoading = false }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Update")
        }

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }
    }
}


fun updateOrderStatus(userId: String, orderDate: String, newStatus: String, onResult: () -> Unit) {
    val url = "https://jvmwk1nved.execute-api.us-east-1.amazonaws.com/orderUpdate"
    val client = OkHttpClient()

    val json = JSONObject().apply {
        put("userId", userId)
        put("orderDate", orderDate)
        put("status", newStatus)
    }

    val requestBody = RequestBody.create(
        "application/json".toMediaTypeOrNull(),
        json.toString()
    )

    val request = Request.Builder()
        .url(url)
        .post(requestBody)
        .addHeader("Content-Type", "application/json")
        .build()

    CoroutineScope(Dispatchers.IO).launch {
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("Update failed: ${e.message}")
                onResult()
            }

            override fun onResponse(call: Call, response: Response) {
                println("Update response: ${response.code}")
                onResult()
            }
        })
    }
}
