package com.example.fitness.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.fitness.ui.theme.AppColors
import com.example.fitness.ui.theme.PremiumButton
import com.example.fitness.ui.theme.TechColors
import com.example.fitness.ui.theme.glassEffect
import com.example.fitness.ui.theme.neonGlowBorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun BodyPhotoScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    // store Pair(uri, formattedTimestamp)
    val photos = remember { mutableStateListOf<Pair<Uri, String>>() }

    // helper formatter for display
    val displayFmt = remember { SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()) }
    val fileNameParseFmt = remember { SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()) }

    // helper to parse year/month from the display timestamp
    fun yearMonthFromTs(ts: String): Pair<Int, Int>? {
        return try {
            val d = displayFmt.parse(ts) ?: return null
            val cal = Calendar.getInstance()
            cal.time = d
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH) + 1 // 1-12
            Pair(y, m)
        } catch (_: Exception) {
            null
        }
    }

    // preload existing files from external files dir
    LaunchedEffect(Unit) {
        val storageDir = ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (storageDir != null && storageDir.exists()) {
            val files: List<File> = withContext(Dispatchers.IO) {
                storageDir.listFiles()?.sortedByDescending { it.lastModified() }?.toList() ?: emptyList()
            }
            val filenameRegex = Regex("\\d{8}_\\d{6}")
            files.forEach { f ->
                val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f)
                // try to extract timestamp from filename BODY_yyyyMMdd_HHmmss
                val tsText = try {
                    val name = f.name
                    val match = filenameRegex.find(name)
                    if (match != null) {
                        val part = match.value
                        val d = fileNameParseFmt.parse(part)
                        if (d != null) displayFmt.format(d) else displayFmt.format(Date(f.lastModified()))
                    } else {
                        displayFmt.format(Date(f.lastModified()))
                    }
                } catch (_: Exception) {
                    displayFmt.format(Date(f.lastModified()))
                }
                if (!photos.any { it.first == uri }) photos.add(Pair(uri, tsText))
            }
        }
    }

    // determine initial selected month (current)
    val nowCal = Calendar.getInstance()
    var selectedYear by remember { mutableStateOf(nowCal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(nowCal.get(Calendar.MONTH) + 1) }
    var showMonthMenu by remember { mutableStateOf(false) }

    // derived list of distinct month-year pairs
    val monthsAvailable = remember(photos) {
        photos.mapNotNull { yearMonthFromTs(it.second) }
            .distinct()
            .sortedWith(compareByDescending<Pair<Int, Int>> { it.first }.thenByDescending { it.second })
    }

    fun monthLabel(y: Int, m: Int) = String.format(Locale.getDefault(), "%d 年 %02d 月", y, m)

    fun goPrevMonth() {
        if (selectedMonth == 1) {
            selectedYear -= 1
            selectedMonth = 12
        } else selectedMonth -= 1
    }

    fun goNextMonth() {
        if (selectedMonth == 12) {
            selectedYear += 1
            selectedMonth = 1
        } else selectedMonth += 1
    }

    // Create file logic
    fun createImageFile(): Pair<File?, String?> {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (storageDir == null) return Pair(null, null)
            if (!storageDir.exists()) storageDir.mkdirs()

            var file = File(storageDir, "BODY_${timeStamp}.jpg")
            var suffix = 1
            while (file.exists()) {
                file = File(storageDir, "BODY_${timeStamp}_$suffix.jpg")
                suffix++
            }
            file.createNewFile()
            val parsed = try { fileNameParseFmt.parse(timeStamp) } catch (_: Exception) { null }
            val display = if (parsed != null) displayFmt.format(parsed) else displayFmt.format(Date())
            Pair(file, display)
        } catch (_: IOException) {
            Pair(null, null)
        }
    }

    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var photoTimestamp by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success && photoUri != null) {
            photos.add(0, Pair(photoUri!!, photoTimestamp ?: displayFmt.format(Date())))
        }
    }

    // --- UI Layout ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(TechColors.DarkBlue, TechColors.DeepPurple)
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // 1. Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassEffect(cornerRadius = 20.dp)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDone) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = TechColors.NeonBlue
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Body Gallery",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TechColors.NeonBlue
                        )
                        Text(
                            text = "記錄體態變化",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Actions (Camera & Gallery)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PremiumButton(
                    text = "拍照記錄",
                    onClick = {
                        val (file, ts) = createImageFile()
                        if (file != null) {
                            val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", file)
                            photoUri = uri
                            photoTimestamp = ts
                            launcher.launch(uri)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CameraAlt,
                    gradient = AppColors.GradientPrimary
                )

                OutlinedButton(
                    onClick = {
                        if (photos.isNotEmpty()) {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(photos.first().first, "image/*")
                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            }
                            ctx.startActivity(intent)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .neonGlowBorder(cornerRadius = 12.dp, borderWidth = 1.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TechColors.NeonBlue)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("最近照片")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Month Switcher
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassEffect(cornerRadius = 16.dp)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { goPrevMonth() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev", tint = Color.White)
                    }

                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showMonthMenu = true }
                                .padding(8.dp)
                        ) {
                            Text(
                                text = monthLabel(selectedYear, selectedMonth),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TechColors.NeonBlue
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = TechColors.NeonBlue)
                        }

                        DropdownMenu(
                            expanded = showMonthMenu,
                            onDismissRequest = { showMonthMenu = false },
                            containerColor = TechColors.DarkBlue.copy(alpha = 0.95f)
                        ) {
                            if (monthsAvailable.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("暫無紀錄", color = Color.Gray) },
                                    onClick = { showMonthMenu = false }
                                )
                            } else {
                                monthsAvailable.forEach { (y, m) ->
                                    DropdownMenuItem(
                                        text = { Text(monthLabel(y, m), color = Color.White) },
                                        onClick = {
                                            selectedYear = y
                                            selectedMonth = m
                                            showMonthMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    IconButton(onClick = { goNextMonth() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.graphicsLayer(rotationZ = 180f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Photo List
            val filteredPhotos = photos.filter { pair ->
                val ym = yearMonthFromTs(pair.second)
                ym?.let { it.first == selectedYear && it.second == selectedMonth } ?: false
            }

            if (filteredPhotos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "本月尚無照片",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(items = filteredPhotos, key = { it.first.toString() }) { pair ->
                        PhotoCard(
                            uri = pair.first,
                            timestamp = pair.second,
                            onDelete = { photos.remove(pair) },
                            onView = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(pair.first, "image/*")
                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                }
                                ctx.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoCard(
    uri: Uri,
    timestamp: String,
    onDelete: () -> Unit,
    onView: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassEffect(cornerRadius = 16.dp)
    ) {
        Column {
            // Image Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .clickable(onClick = onView)
            ) {
                UriImage(
                    uri = uri,
                    modifier = Modifier.fillMaxSize()
                )

                // Timestamp Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                        .padding(12.dp)
                ) {
                    Text(
                        text = timestamp,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF6B6B))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("刪除照片")
                }
            }
        }
    }
}

@Composable
private fun UriImage(uri: Uri, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var imageBitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            try {
                ctx.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        imageBitmap = bmp.asImageBitmap()
                    }
                }
            } catch (_: Exception) {
                // ignore
            }
        }
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap!!,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = TechColors.NeonBlue,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}