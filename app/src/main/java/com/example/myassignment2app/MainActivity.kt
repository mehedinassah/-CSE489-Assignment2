package com.example.myassignment2app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.MotionEvent
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.example.myassignment2app.ui.theme.MyAssignment2AppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyAssignment2AppTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedScreen by remember { mutableStateOf("Broadcast Receiver") }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Menu",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                listOf(
                    "Broadcast Receiver",
                    "Image Scale",
                    "Video",
                    "Audio"
                ).forEach { item ->
                    NavigationDrawerItem(
                        label = { Text(item) },
                        selected = selectedScreen == item,
                        onClick = {
                            selectedScreen = item
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(selectedScreen) },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (selectedScreen) {
                    "Broadcast Receiver" -> BroadcastReceiverScreen()
                    "Image Scale" -> ImageScaleScreen()
                    "Video" -> VideoScreen()
                    "Audio" -> AudioScreen()
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// BROADCAST RECEIVER SCREEN (Screen 1)
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastReceiverScreen() {
    val options = listOf("Custom broadcast receiver", "System battery notification receiver")
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf(options[0]) }
    var currentScreen by remember { mutableStateOf("") }

    when {
        currentScreen == "custom_input" -> {
            CustomBroadcastInputScreen(onSend = { msg ->
                currentScreen = "custom_receiver:$msg"
            })
        }
        currentScreen.startsWith("custom_receiver:") -> {
            val msg = currentScreen.removePrefix("custom_receiver:")
            CustomBroadcastReceiverScreen(message = msg)
        }
        currentScreen == "battery" -> {
            BatteryScreen()
        }
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select a broadcast type",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedOption,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedOption = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = {
                    currentScreen = if (selectedOption == options[0]) "custom_input" else "battery"
                }) {
                    Text("Proceed")
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// CUSTOM BROADCAST — INPUT SCREEN (Screen 2)
// ─────────────────────────────────────────────
@Composable
fun CustomBroadcastInputScreen(onSend: (String) -> Unit = {}) {
    var message by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Enter your message",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Message") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { onSend(message) }) {
            Text("Send Broadcast")
        }
    }
}

// ─────────────────────────────────────────────
// CUSTOM BROADCAST — RECEIVER SCREEN (Screen 3)
// ─────────────────────────────────────────────
@Composable
fun CustomBroadcastReceiverScreen(message: String) {
    var receivedMessage by remember { mutableStateOf("Waiting...") }
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                receivedMessage = intent?.getStringExtra("MESSAGE") ?: "No message"
            }
        }
        val filter = IntentFilter("com.example.myassignment2app.CUSTOM_BROADCAST")
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)

        // Send broadcast AFTER receiver is registered, with a small delay
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val broadcastIntent = Intent("com.example.myassignment2app.CUSTOM_BROADCAST")
            broadcastIntent.putExtra("MESSAGE", message)
            context.sendBroadcast(broadcastIntent)
        }, 200)

        onDispose { context.unregisterReceiver(receiver) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Broadcast Received!", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Message: $receivedMessage", style = MaterialTheme.typography.bodyLarge)
    }
}

// ─────────────────────────────────────────────
// BATTERY SCREEN
// ─────────────────────────────────────────────
@Composable
fun BatteryScreen() {
    var batteryLevel by remember { mutableStateOf("Reading...") }
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
                val pct = (level * 100 / scale.toFloat()).toInt()
                batteryLevel = "$pct%"
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Battery Level", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text(batteryLevel, style = MaterialTheme.typography.displayMedium)
    }
}

// ─────────────────────────────────────────────
// IMAGE SCALE SCREEN
// ─────────────────────────────────────────────
@Composable
fun ImageScaleScreen() {
    val context = LocalContext.current

    AndroidView(
        factory = {
            val imageView = ImageView(context).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                Glide.with(context)
                    .load("https://picsum.photos/800/600")
                    .into(this)
            }

            var scale = 1f
            val scaleDetector = android.view.ScaleGestureDetector(
                context,
                object : android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    override fun onScale(detector: android.view.ScaleGestureDetector): Boolean {
                        scale *= detector.scaleFactor
                        scale = scale.coerceIn(0.5f, 5f)
                        imageView.scaleX = scale
                        imageView.scaleY = scale
                        return true
                    }
                }
            )

            imageView.setOnTouchListener { v, event ->
                scaleDetector.onTouchEvent(event)
                v.performClick()
                true
            }

            imageView
        },
        modifier = Modifier.fillMaxSize()


    )
}

// ─────────────────────────────────────────────
// VIDEO SCREEN
// ─────────────────────────────────────────────
@Composable
fun VideoScreen() {
    val context = LocalContext.current

    AndroidView(
        factory = {
            android.widget.VideoView(context).apply {
                val uri = android.net.Uri.parse(
                    "android.resource://${context.packageName}/${R.raw.sample_video}"
                )
                setVideoURI(uri)

                val mediaController = android.widget.MediaController(context)
                mediaController.setAnchorView(this)
                setMediaController(mediaController)

                setOnPreparedListener { mp ->
                    mp.isLooping = false
                    start()
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

// ─────────────────────────────────────────────
// AUDIO SCREEN
// ─────────────────────────────────────────────
@Composable
fun AudioScreen() {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }

    val mediaPlayer = remember {
        android.media.MediaPlayer.create(context, R.raw.sample_audio)
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isPlaying) "Playing..." else "Paused",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = {
                if (!isPlaying) {
                    mediaPlayer.start()
                    isPlaying = true
                }
            }) {
                Text("Play")
            }

            Button(onClick = {
                if (isPlaying) {
                    mediaPlayer.pause()
                    isPlaying = false
                }
            }) {
                Text("Pause")
            }

            Button(onClick = {
                mediaPlayer.stop()
                mediaPlayer.prepare()
                isPlaying = false
            }) {
                Text("Stop")
            }
        }
    }
}