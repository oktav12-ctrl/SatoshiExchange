package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LogType
import com.example.data.SyncLog
import com.example.data.SyncStatus
import com.example.data.Task
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Custom modern palette colors
val ObsidianDarkBg = Color(0xFF0F172A)     // Deep Slate Black
val SurfaceSlateSteel = Color(0xFF1E293B)  // M3 Card Surface Blue-Gray
val VividTeal = Color(0xFF0D9488)          // Calm Teal Primary
val ElectricSky = Color(0xFF38BDF8)        // Radiant Sky Accent
val AlertCoral = Color(0xFFEF4444)         // Bright Crimson
val WarningAmber = Color(0xFFF59E0B)       // Sunset Gold Warning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskApp(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasks.collectAsState()
    val isOnline by viewModel.isDeviceOnline.collectAsState()
    val isAudioEnabled by viewModel.isAudioEnabled.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val logs by viewModel.syncLogsList.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()

    // Auto scroll the sync console terminal to bottom upon receiving new logs
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            lazyListState.animateScrollToItem(logs.size - 1)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = if (isOnline) ElectricSky else Color.LightGray,
                            modifier = Modifier.size(26.dp)
                        )
                        Text(
                            text = "Task Manager",
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                            color = if (isSystemInDarkTheme()) Color.White else ObsidianDarkBg
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.triggerSyncNow() },
                        enabled = !isSyncing,
                        modifier = Modifier.testTag("sync_toolbar_button")
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                color = VividTeal,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Manual Database Sync",
                                tint = VividTeal
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = VividTeal,
                contentColor = Color.White,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("add_task_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Local Task",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Simulated Connectivity Dashboard Controller Frame
            ConnectivityHeader(
                isOnline = isOnline,
                isAudioEnabled = isAudioEnabled,
                isSyncing = isSyncing,
                onToggleOnline = { viewModel.toggleOnlineSimulator(it) },
                onToggleAudio = { viewModel.toggleAudioFeedback(it) },
                onSyncNow = { viewModel.triggerSyncNow() }
            )

            // Primary Screen Content Grid Split
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "My Task Board",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                if (tasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = VividTeal.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(VividTeal.copy(alpha = 0.1f), CircleShape)
                                    .padding(12.dp)
                            )
                            Text(
                                text = "All tasks cleared!",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Create tasks offline anytime. They will cache locally and publish seamlessly once synced online.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxWidth()
                            .testTag("task_list_container"),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(tasks, key = { it.id }) { task ->
                            TaskRowItem(
                                task = task,
                                onToggleComplete = { viewModel.toggleTaskCompletion(task.id, !task.isCompleted) },
                                onDelete = { viewModel.removeTask(task.id) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Diagnostic Command Console Pane (Streaming logs visualization)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sync Telemetry Console",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Clear Logs",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = VividTeal,
                            modifier = Modifier
                                .clickable { viewModel.clearLogStream() }
                                .padding(4.dp)
                        )
                        Text(
                            text = "Purge DB",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AlertCoral,
                            modifier = Modifier
                                .clickable { showDeleteConfirmDialog = true }
                                .padding(4.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(0.7f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF020617)) // Clean Terminal jet black
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    if (logs.isEmpty()) {
                        Text(
                            text = "Shell waiting for telemetry input...",
                            color = Color.DarkGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(logs) { log ->
                                ConsoleLogLine(log = log)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Modal Trigger Framework: Create Task Dialogue
    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, desc ->
                viewModel.addNewTask(title, desc)
                showAddTaskDialog = false
            }
        )
    }

    // Modal Reset Framework
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Hard Database Reset?") },
            text = { Text("This completely purges SQLite local records and clears the diagnostic console. This is an irreversible debug operation.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.purgeDatabase()
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text("Deconstruct ALL Data", color = AlertCoral)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ConnectivityHeader(
    isOnline: Boolean,
    isAudioEnabled: Boolean,
    isSyncing: Boolean,
    onToggleOnline: (Boolean) -> Unit,
    onToggleAudio: (Boolean) -> Unit,
    onSyncNow: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSystemInDarkTheme()) SurfaceSlateSteel else Color(0xFFF1F5F9)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Simulated Connectivity Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Simulated Connectivity",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isOnline) Color(0xFF10B981) else Color(0xFFEF4444), CircleShape)
                        )
                        Text(
                            text = if (isOnline) "Cloud Endpoint Linked" else "Device Offline",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isOnline) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    }
                }

                Switch(
                    checked = isOnline,
                    onCheckedChange = onToggleOnline,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = VividTeal,
                        uncheckedThumbColor = Color.LightGray,
                        uncheckedTrackColor = Color.DarkGray
                    ),
                    modifier = Modifier.testTag("connectivity_switch")
                )
            }

            // Audio Feedbacks / Suara Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Audio Feedback / Suara",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = if (isAudioEnabled) VividTeal else Color.LightGray.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isAudioEnabled) "Sound Confirms On" else "Sound Confirms Muted",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isAudioEnabled) VividTeal else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isAudioEnabled,
                    onCheckedChange = onToggleAudio,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = VividTeal,
                        uncheckedThumbColor = Color.LightGray,
                        uncheckedTrackColor = Color.DarkGray
                    ),
                    modifier = Modifier.testTag("sound_switch")
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Core Engine State",
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = when {
                            isSyncing -> "Flushing local dirty cache..."
                            isOnline -> "Holding idle synchronization pipe"
                            else -> "Offline queue accumulating data changes"
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onSyncNow,
                    enabled = !isSyncing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VividTeal,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("sync_action_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Sync NOW", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TaskRowItem(
    task: Task,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val cardBgColor by animateColorAsState(
        targetValue = if (task.isCompleted) {
            if (isSystemInDarkTheme()) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFFF8FAFC)
        } else {
            if (isSystemInDarkTheme()) SurfaceSlateSteel else Color.White
        },
        label = "TaskCardColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_row_${task.id}"),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (task.isCompleted) {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            } else {
                when (task.syncStatus) {
                    SyncStatus.PENDING_CREATE -> WarningAmber.copy(alpha = 0.4f)
                    SyncStatus.PENDING_UPDATE -> ElectricSky.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                }
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Task Checkbox Box Target (48dp Touch Scale compliance)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onToggleComplete() }
                    .testTag("task_check_${task.id}"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (task.isCompleted) VividTeal else Color.Transparent
                        )
                        .border(
                            width = 2.dp,
                            color = if (task.isCompleted) VividTeal else Color.Gray,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Task Header and Sync Meta Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = task.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Custom Sync Status Pill badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val badgeColor = when (task.syncStatus) {
                        SyncStatus.SYNCED -> VividTeal
                        SyncStatus.PENDING_CREATE -> WarningAmber
                        SyncStatus.PENDING_UPDATE -> ElectricSky
                        SyncStatus.PENDING_DELETE -> AlertCoral
                    }

                    val badgeLabel = when (task.syncStatus) {
                        SyncStatus.SYNCED -> "☁ Synced"
                        SyncStatus.PENDING_CREATE -> "⚡ Offline Pending"
                        SyncStatus.PENDING_UPDATE -> "⇵ Updates Queued"
                        SyncStatus.PENDING_DELETE -> "🗑 Pending Purge"
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeColor
                        )
                    }

                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    Text(
                        text = sdf.format(Date(task.createdAt)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }

            val context = androidx.compose.ui.platform.LocalContext.current

            // WhatsApp Share Button
            IconButton(
                onClick = {
                    val statusEmoji = if (task.isCompleted) "✅ [Selesai]" else "⏳ [Tertunda]"
                    val descriptionText = if (task.description.isNotBlank()) "\nCatatan: ${task.description}" else ""
                    val message = "*Task Manager - Pengingat Tugas*\nTugas: *${task.title}*$descriptionText\nStatus: $statusEmoji\n\nSent via Android Task Manager with Offline Sync."
                    
                    val sendIntent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(android.content.Intent.EXTRA_TEXT, message)
                        type = "text/plain"
                        `package` = "com.whatsapp"
                    }
                    try {
                        context.startActivity(sendIntent)
                    } catch (e: Exception) {
                        // Fallback chooser if standard WhatsApp app package isn't uniquely launched
                        val chooser = android.content.Intent.createChooser(
                            android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, message)
                                type = "text/plain"
                            },
                            "Bagikan Tugas via WhatsApp / Lainnya"
                        )
                        context.startActivity(chooser)
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("task_share_wa_${task.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share on WhatsApp",
                    tint = Color(0xFF25D366), // WhatsApp Brand Green
                    modifier = Modifier.size(20.dp)
                )
            }

            // Trash button for removal
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("task_delete_${task.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove task locally",
                    tint = AlertCoral.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ConsoleLogLine(log: SyncLog) {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val formattedTime = sdf.format(Date(log.timestamp))

    val typeColor = when (log.type) {
        LogType.SUCCESS -> Color(0xFF10B981) // Rich green
        LogType.WARNING -> WarningAmber
        LogType.ERROR -> AlertCoral
        LogType.INFO -> ElectricSky
    }

    val typePrefix = when (log.type) {
        LogType.SUCCESS -> "[SECURE-OK]"
        LogType.WARNING -> "[SYS-WARN]"
        LogType.ERROR -> "[ERR-CRIT]"
        LogType.INFO -> "[INFO-TRC]"
    }

    Text(
        text = "$formattedTime $typePrefix ${log.message}",
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        color = typeColor,
        lineHeight = 14.sp
    )
}

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    val controller = LocalSoftwareKeyboardController.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assemble New Task", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Header") },
                    placeholder = { Text("Buy groceries...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_task_title_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VividTeal,
                        focusedLabelColor = VividTeal
                    )
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Optional Scope/Notes") },
                    placeholder = { Text("2l whole milk, bread, organic apples...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_task_desc_input"),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VividTeal,
                        focusedLabelColor = VividTeal
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        controller?.hide()
                        onConfirm(title, desc)
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = VividTeal),
                modifier = Modifier.testTag("add_task_confirm_btn")
            ) {
                Text("Enforce List", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("add_task_cancel_btn")
            ) {
                Text("Postpone")
            }
        }
    )
}
