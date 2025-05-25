package com.kotarokase.administrator

import  android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import retrofit2.converter.gson.GsonConverterFactory
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import kotlinx.coroutines.withContext
import retrofit2.http.Body
import retrofit2.http.POST
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import androidx.compose.material3.ExperimentalMaterial3Api
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import retrofit2.Response



fun uploadItemToServer(
    context: Context,
    itemName: String,
    itemDescription: String,
    itemPrice: String,
    tag: String,
    imageUris: List<Uri>,
    onLog: (String) -> Unit,
    onFinished: () -> Unit
) {
    val itemId = UUID.randomUUID().toString()
    val storage = FirebaseStorage.getInstance().reference

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val imageUrls = mutableListOf<String>()

            for ((index, uri) in imageUris.withIndex()) {
                val imageRef = storage.child("items/$itemId/image$index.jpg")
                imageRef.putFile(uri).await()
                println("adding new url "+imageRef.downloadUrl.await().toString())

                val url = imageRef.downloadUrl.await().toString()
                imageUrls.add(url)

            }
            println("uploaded successfully" + itemPrice.toInt(),)

            val data = ItemUploadRequest(
                itemId = itemId,
                itemName = itemName,
                itemDescription = itemDescription,
                itemPrice = itemPrice.toInt(),
                tag = tag,
                uploadDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                imageUrl = imageUrls.getOrNull(0) ?: "",
                imageUrl1 = imageUrls.getOrNull(1) ?: "",
                imageUrl2 = imageUrls.getOrNull(2) ?: "",
                imageUrl3 = imageUrls.getOrNull(3) ?: ""
            )


            println("successfully made instance")

            val retrofit = Retrofit.Builder()
                .baseUrl("https://jvmwk1nved.execute-api.us-east-1.amazonaws.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()


            println("break point1")

            val api = retrofit.create(ItemApiService::class.java)
            println("break point1")
            val response = withContext(Dispatchers.IO) {
                api.uploadItem(data)
            }

            println("successfully sent to server")

            withContext(Dispatchers.Main) {
                Toast.makeText(context, if (response.isSuccessful) "success" else "failed" , Toast.LENGTH_SHORT).show()
                onFinished()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
                onFinished()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadItemScreen(navController: NavController) {
    var logMessage by remember { mutableStateOf("") }

    val context = LocalContext.current
    val imageUris = remember { mutableStateListOf<Uri?>(null, null, null, null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val firstEmptyIndex = imageUris.indexOfFirst { it == null }
        if (firstEmptyIndex != -1 && uri != null) {
            imageUris[firstEmptyIndex] = uri
        }
    }

    var itemName by remember { mutableStateOf("") }
    var itemDescription by remember { mutableStateOf("") }
    var itemPrice by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("king") }
    val tagOptions = listOf("king", "premium", "sports")
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) {innerPadding->
        Column(Modifier
            .padding(innerPadding)
            .padding(16.dp)) {
            OutlinedTextField(
                value = itemName,
                onValueChange = { itemName = it },
                label = { Text("Item Name") })
            OutlinedTextField(
                value = itemDescription,
                onValueChange = { itemDescription = it },
                label = { Text("Description") })
            OutlinedTextField(
                value = itemPrice,
                onValueChange = { itemPrice = it.filter { it.isDigit() } },
                label = { Text("Price") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            var expanded by remember { mutableStateOf(false) }

            Box {
                Button(onClick = { expanded = true }) {
                    Text("Tag: $selectedTag")
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    tagOptions.forEach { tag ->
                        DropdownMenuItem(
                            text = { Text(tag) },
                            onClick = {
                                selectedTag = tag
                                expanded = false
                            }
                        )
                    }
                }
            }


            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                imageUris.forEachIndexed { index, uri ->
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clickable { launcher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (uri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = null
                            )
                        } else {
                            Icon(Icons.Default.Add, contentDescription = "Add Image")
                        }
                    }
                }
            }

            Button(
                onClick = {
                    isLoading = true

                    uploadItemToServer(
                        context = context,
                        itemName = itemName,
                        itemDescription = itemDescription,
                        itemPrice = itemPrice,
                        tag = selectedTag,
                        onLog = { message -> logMessage = message },
                        imageUris = imageUris.filterNotNull().toList(),
                    ) {
                        isLoading = false
                        navController.popBackStack()
                    }
                },
                enabled = !isLoading
            ) {
                Text(if (isLoading) "Uploading..." else "Upload Item")
            }
        }
    }
}

interface ItemApiService {
    @POST("addItem")
    suspend fun uploadItem(@Body data: ItemUploadRequest): Response<Void>
}

data class ItemUploadRequest(
    val itemId: String,
    val itemName: String,
    val itemDescription: String,
    val itemPrice: Int,
    val tag: String,
    val uploadDate: String,
    val imageUrl: String,
    val imageUrl1: String,
    val imageUrl2: String,
    val imageUrl3: String
)
