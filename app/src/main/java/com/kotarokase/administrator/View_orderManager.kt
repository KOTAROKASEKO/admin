package com.kotarokase.administrator

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import androidx.compose.foundation.lazy.items
import androidx.navigation.NavController
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Query

data class OrderItem(
    val userId: String,
    val orderDate: String,
    val itemId: String,
    val orderId: String,
    val quantity: Int,
    val status: String
)

interface OrderApiService {
    @GET("fetchOrder")
    fun getOrdersByStatus(@Query("status") status: String): Call<List<OrderItem>>

}

object RetrofitClient {
    private const val BASE_URL = "https://jvmwk1nved.execute-api.us-east-1.amazonaws.com/"

    val instance: OrderApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OrderApiService::class.java)
    }
}


@Composable
fun AdminOrderListScreen(navController: NavController) {
    var orderList by remember { mutableStateOf<List<OrderItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedStatus by remember { mutableStateOf("ordered") }

    val statuses = listOf("ordered", "shipped", "completed", "cancelled")

    fun fetchOrdersByStatus(status: String) {
        isLoading = true
        RetrofitClient.instance.getOrdersByStatus(status).enqueue(object : Callback<List<OrderItem>> {
            override fun onResponse(call: Call<List<OrderItem>>, response: Response<List<OrderItem>>) {
                if (response.isSuccessful) {
                    orderList = response.body() ?: emptyList()
                } else {
                    Log.e("OrderList", "Error: ${response.code()}")
                }
                isLoading = false
            }

            override fun onFailure(call: Call<List<OrderItem>>, t: Throwable) {
                Log.e("OrderList", "Failure: ${t.message}")
                isLoading = false
            }
        })
    }

    // Fetch initial orders
    LaunchedEffect(Unit) {
        fetchOrdersByStatus(selectedStatus)
    }

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp)
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                items(statuses) { status ->
                    val isSelected = status == selectedStatus
                    Button(
                        onClick = {
                            selectedStatus = status
                            fetchOrdersByStatus(status)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(status.replaceFirstChar { it.uppercase() })
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn {
                    items(orderList) { order ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable {
                                    ItemViewModel.selectedItem = order
                                    navController.navigate("detailOrderView")
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Order ID: ${order.orderId}", style = MaterialTheme.typography.titleMedium)
                                Text("User ID: ${order.userId}")
                                Text("Item ID: ${order.itemId}")
                                Text("Quantity: ${order.quantity}")
                                Text("Status: ${order.status}")
                                Text("Date: ${order.orderDate}")
                            }
                        }
                    }
                }
            }
        }
    }
}
