package com.embedhub.app.ui.screens.code

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.embedhub.app.core.ota.EspOtaUpdater
import com.embedhub.app.core.ota.OtaProgress
import com.embedhub.app.core.templates.CodeTemplates
import com.embedhub.app.core.templates.SketchTemplate
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun CodeScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Arduino/ESP Sketches", "OTA Firmware Flashing")

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title, fontSize = 13.sp) }
                )
            }
        }

        if (selectedTab == 0) {
            SketchesTab()
        } else {
            OtaFlashingTab()
        }
    }
}

@Composable
fun SketchesTab() {
    val context = LocalContext.current
    var selectedTemplate by remember { mutableStateOf<SketchTemplate?>(CodeTemplates.allTemplates.firstOrNull()) }

    Row(modifier = Modifier.fillMaxSize()) {
        // Sidebar templates list
        LazyColumn(
            modifier = Modifier
                .width(180.dp)
                .fillMaxHeight()
                .background(Color(0xFF1E293B))
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(CodeTemplates.allTemplates) { template ->
                val isSelected = template.id == selectedTemplate?.id
                Card(
                    onClick = { selectedTemplate = template },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(template.title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                        Text(template.board, fontSize = 9.sp, color = Color.Gray)
                    }
                }
            }
        }

        // Code Viewer Area
        selectedTemplate?.let { template ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(template.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(template.description, fontSize = 11.sp, color = Color.LightGray)
                    }

                    Row {
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Arduino Code", template.code))
                            Toast.makeText(context, "Copied sketch to clipboard!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFF38BDF8))
                        }
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, template.title)
                                putExtra(Intent.EXTRA_TEXT, template.code)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Sketch"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color(0xFF38BDF8))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = template.code,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OtaFlashingTab() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var espIp by remember { mutableStateOf("192.168.4.1") }
    var espPort by remember { mutableStateOf("80") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("firmware.bin") }
    var progressPercent by remember { mutableStateOf(0) }
    var isUploading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            selectedFileName = uri.lastPathSegment ?: "firmware.bin"
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("ESP8266 & ESP32 HTTP OTA Firmware Updater", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Flash compiled Arduino/ESP binary (.bin) files over WiFi without USB cables.", fontSize = 12.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = espIp,
                onValueChange = { espIp = it },
                label = { Text("ESP IP Address") },
                modifier = Modifier.weight(2f)
            )
            OutlinedTextField(
                value = espPort,
                onValueChange = { espPort = it },
                label = { Text("Port") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Firmware Binary File:", fontSize = 12.sp, color = Color.Gray)
                    Text(selectedFileName, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                }
                OutlinedButton(onClick = { filePickerLauncher.launch("*/*") }) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Select .bin")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isUploading) {
            LinearProgressIndicator(
                progress = { progressPercent / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text("Uploading OTA Firmware: $progressPercent%", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val uri = selectedFileUri
                if (uri == null) {
                    Toast.makeText(context, "Please pick a .bin file first", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                coroutineScope.launch {
                    isUploading = true
                    statusMessage = "Starting OTA upload to $espIp..."

                    try {
                        // Copy URI stream to temp file
                        val tempFile = File(context.cacheDir, "update.bin")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(tempFile).use { output ->
                                input.copyTo(output)
                            }
                        }

                        val updater = EspOtaUpdater()
                        updater.uploadFirmware(espIp, espPort.toIntOrNull() ?: 80, tempFile).collect { progress ->
                            when (progress) {
                                is OtaProgress.Progress -> {
                                    progressPercent = progress.percentage
                                }
                                is OtaProgress.Success -> {
                                    isUploading = false
                                    statusMessage = "SUCCESS: ${progress.responseMessage}. Board is rebooting!"
                                }
                                is OtaProgress.Failure -> {
                                    isUploading = false
                                    statusMessage = "ERROR: ${progress.error}"
                                }
                            }
                        }
                    } catch (e: Exception) {
                        isUploading = false
                        statusMessage = "Failed: ${e.message}"
                    }
                }
            },
            enabled = !isUploading,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Icon(Icons.Default.CloudUpload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Upload Firmware (OTA Flash)", fontWeight = FontWeight.Bold)
        }

        if (statusMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(statusMessage, color = Color.White, fontWeight = FontWeight.Medium)
        }
    }
}
