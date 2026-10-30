package com.embedhub.app.ui.screens.profiles

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedhub.app.core.manager.ConnectionManager
import com.embedhub.app.core.model.ConnectionConfig
import com.embedhub.app.core.model.ControlProfile
import com.embedhub.app.core.model.LinkType
import com.embedhub.app.data.db.FullProfile
import com.embedhub.app.data.repository.ProfileRepository
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun ProfilesScreen(
    connectionManager: ConnectionManager,
    profileRepository: ProfileRepository,
    selectedSlot: Int
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val profiles by profileRepository.allProfiles.collectAsState(initial = emptyList())

    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)).padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Hardware Profiles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = { showImportDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import JSON", fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val newProf = FullProfile(
                                id = UUID.randomUUID().toString(),
                                name = "Profile ${profiles.size + 1}",
                                slotIndex = selectedSlot,
                                config = ConnectionConfig(linkType = LinkType.BLE)
                            )
                            profileRepository.saveProfile(newProf)
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Profile", fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (profiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No saved profiles. Tap 'New Profile' to save connection and control mappings.", color = Color.Gray, fontSize = 13.sp)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(profiles, key = { it.id }) { profile ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(profile.name, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Transport: ${profile.config.linkType} • Slot ${profile.slotIndex + 1}", fontSize = 12.sp, color = Color(0xFF38BDF8))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Load profile to connection manager
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        connectionManager.connect(profile.slotIndex, profile.config)
                                        Toast.makeText(context, "Loaded & Connecting '${profile.name}'", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Connect", fontSize = 11.sp)
                            }

                            // Duplicate
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    profileRepository.duplicateProfile(profile.id, "${profile.name} (Copy)")
                                }
                            }) {
                                Icon(Icons.Default.CopyAll, contentDescription = "Duplicate", tint = Color.LightGray)
                            }

                            // Export JSON
                            IconButton(onClick = {
                                val jsonStr = profileRepository.exportProfileJson(profile)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("EmbedHub Profile", jsonStr))
                                Toast.makeText(context, "Profile JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Export", tint = Color.LightGray)
                            }

                            // Delete
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    profileRepository.deleteProfile(profile.id)
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Profile JSON") },
            text = {
                OutlinedTextField(
                    value = importJsonText,
                    onValueChange = { importJsonText = it },
                    label = { Text("Paste JSON string") },
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                )
            },
            confirmButton = {
                Button(onClick = {
                    val res = profileRepository.importProfileJson(importJsonText)
                    if (res.isSuccess) {
                        coroutineScope.launch {
                            profileRepository.saveProfile(res.getOrThrow())
                            showImportDialog = false
                            importJsonText = ""
                        }
                    } else {
                        Toast.makeText(context, "Invalid profile JSON format", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
