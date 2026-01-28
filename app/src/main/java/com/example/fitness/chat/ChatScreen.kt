package com.example.fitness.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.fitness.BuildConfig
import com.example.fitness.network.GeminiClient
import kotlinx.coroutines.launch
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.delay

@Composable
fun ChatScreen() {
    val messages = remember { mutableStateListOf<Pair<String, String>>() }
    var input by remember { mutableStateOf(TextFieldValue("")) }
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var apiKeyMissing by remember { mutableStateOf(false) }
    var lastDebugInfo by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // simple check for API key availability
    LaunchedEffect(Unit) {
        apiKeyMissing = BuildConfig.GEMINI_API_KEY.isBlank()
        if (!apiKeyMissing) {
            delay(120)
            focusRequester.requestFocus()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text(text = "訓練助手", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Card(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("尚無對話，輸入訊息開始與訓練助手聊天")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(messages) { (sender, text) ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (sender == "user") Arrangement.End else Arrangement.Start) {
                            val bg = if (sender == "user") MaterialTheme.colorScheme.primary.copy(alpha = 0.9f) else MaterialTheme.colorScheme.surfaceVariant
                            Box(modifier = Modifier
                                .widthIn(max = 280.dp)
                                .background(bg, shape = RoundedCornerShape(8.dp))
                                .padding(8.dp)) {
                                Text(text = text, color = if (sender == "user") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (apiKeyMissing) {
            Text(text = "未設定 Gemini API Key。請在環境變數 GEMINI_API_KEY 或 BuildConfig 中設定 GEMINI_API_KEY。", color = MaterialTheme.colorScheme.error)
        }

        lastDebugInfo?.let { dbg ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Last: $dbg", style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // helper to send messages from button or keyboard
            fun sendMessage() {
                val text = input.text.trim()
                if (text.isEmpty() || loading || apiKeyMissing) return
                messages.add("user" to text)
                input = TextFieldValue("")
                loading = true
                scope.launch {
                    try {
                        val res = GeminiClient.ask(text)
                        if (res.isSuccess) {
                            val display = res.getOrNull() ?: "(無回應)"
                            messages.add("gemini" to display)
                        } else {
                            val msg = res.exceptionOrNull()?.message ?: "Unknown error"
                            messages.add("gemini" to "呼叫 Gemini 發生錯誤: $msg")
                            lastDebugInfo = "ERR: ${msg.take(300)}"
                        }
                    } catch (e: Exception) {
                        val msg = e.message ?: e.toString()
                        messages.add("gemini" to "呼叫 Gemini 發生錯誤: $msg")
                        lastDebugInfo = "ERR: ${msg.take(300)}"
                    } finally {
                        loading = false
                    }
                }
            }

            TextField(
                modifier = Modifier.weight(1f).padding(end = 8.dp).focusRequester(focusRequester),
                value = input,
                onValueChange = { input = it },
                enabled = !loading && !apiKeyMissing,
                placeholder = { Text("輸入訊息...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    sendMessage()
                    keyboardController?.hide()
                })
            )

            Button(onClick = { sendMessage() }, enabled = !loading && !apiKeyMissing) {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp) else Text("傳送")
            }
        }
    }
}
