package com.kotarokase.administrator

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable


import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck

val mainColor =Color(0xFFFF9800)


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )

        setContent {

            val context = this
            var startDestination by remember { mutableStateOf("loadingView") }
            var isLoading by remember { mutableStateOf(true) }
            var isLoggedIn by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                isLoggedIn = PreferenceManager.getLoginStatus(context)
                isLoading = false
            }

            if (isLoading) {
                LoadingView()
            } else {
                if(isLoggedIn){
                    startDestination = "appendItem"
                }else{
                    startDestination = "authView"
                }
                HomeNavigation(startDestination)
            }
        }
    }
}

@Composable
fun HomeNavigation(startDestination: String) {

    val navController = rememberNavController()

    Scaffold(

    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("loadingView"){
                LoadingView()
            }
            composable("authView") {
                AuthView(navController)
            }
            composable("appendItem") {
                UploadItemScreen(navController)
            }
            composable("orderList") {
                AdminOrderListScreen(navController)
            }
            composable("detailOrderView"){
                DetailedOrderView()
            }

        }
    }
}

@Composable
fun LoadingView(
){

    Column {
        AndroidView(
            factory = { ctx ->
                android.widget.ImageView(ctx).apply {
                    val gifDrawable = ContextCompat.getDrawable(ctx, R.drawable.loading)
                    setImageDrawable(gifDrawable)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
            })
        Text(text = "Checking your credentials",modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Profile
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    BottomNavigation {

        items.forEach { item ->
            BottomNavigationItem(
                modifier = Modifier.background(
                    color = mainColor
                ),
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            // バックスタック整理
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}


sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Home : BottomNavItem("appendItem", "Home", Icons.Default.Home)
    object Profile : BottomNavItem("orderList", "Profile", Icons.Default.Person)
}

object PreferenceManager {
    private const val PREFS_NAME = "MyPrefs"

    fun saveLoginStatus(context: Context, isLoggedIn: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("isLoggedIn", isLoggedIn).apply()
    }

    fun getLoginStatus(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("isLoggedIn", false)
    }
}

@Composable
fun AuthView(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val emailState = remember { mutableStateOf("") }
    val passwordState = remember { mutableStateOf("") }
    val passwordVisible = remember { mutableStateOf(false) }
    var errorMessage = remember { mutableStateOf<String?>(null) }
    val isLoading = remember { mutableStateOf(false) }

    if (isLoading.value) {
        LoadingScreen()
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome Back!",
                style = MaterialTheme.typography.h4,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = emailState.value,
                onValueChange = { emailState.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = passwordState.value,
                onValueChange = { passwordState.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible.value = !passwordVisible.value }) {
                        Icon(
                            imageVector = if (passwordVisible.value) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible.value) "Hide password" else "Show password"
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // エラーメッセージ表示
            errorMessage.value?.let { message ->
                Text(
                    text = message,
                    color = Color.Red,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if(emailState.value!=null && emailState.value.isNotEmpty() && !emailState.value.isBlank() &&
                        passwordState.value!=null && passwordState.value.isNotEmpty() && !passwordState.value.isBlank()
                    ){
                        isLoading.value = true
                        Firebase.auth.signInWithEmailAndPassword(
                            emailState.value,
                            passwordState.value
                        ).addOnCompleteListener { task ->
                            isLoading.value = false
                            if (task.isSuccessful) {
                                PreferenceManager.saveLoginStatus(context, true)
                                navController.navigate("appendItem") {
                                    popUpTo("authView") { inclusive = true }
                                }
                            } else {
                                errorMessage.value = task.exception?.localizedMessage ?: "Login failed."
                            }
                        }
                    }else{
                        errorMessage.value = "Please enter email and password."
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Login", style = MaterialTheme.typography.button)
            }

            OutlinedButton(
                onClick = {
                    isLoading.value = true
                    Firebase.auth.createUserWithEmailAndPassword(
                        emailState.value,
                        passwordState.value
                    ).addOnCompleteListener { task ->
                        isLoading.value = false
                        if (task.isSuccessful) {
                            PreferenceManager.saveLoginStatus(context, true)
                            navController.navigate("appendItem") {
                                popUpTo("authView") { inclusive = true }
                            }
                        } else {
                            errorMessage.value = task.exception?.localizedMessage ?: "Signup failed."
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Sign up", style = MaterialTheme.typography.button)
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    val dotCount = remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            dotCount.value = (dotCount.value + 1) % 4
        }
    }

    val dots = when (dotCount.value) {
        0 -> ""
        1 -> "."
        2 -> ".."
        else -> "..."
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Loading$dots",
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold
        )
    }
}







