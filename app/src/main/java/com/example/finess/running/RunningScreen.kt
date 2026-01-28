import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import java.util.Locale

@Composable
fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    label: String,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var tempValue by remember { mutableStateOf(value.coerceIn(range.first, range.last)) }

    // Show the selected value in a readOnly text field with a dropdown icon; make the field clickable
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value.toString(),
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
                .clickable {
                    tempValue = value.coerceIn(range.first, range.last)
                    showDialog = true
                }
        )
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = "dropdown",
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .clickable {
                    tempValue = value.coerceIn(range.first, range.last)
                    showDialog = true
                }
        )
    }

    if (showDialog) {
        // Dialog contains a native Android NumberPicker as a wheel using AndroidView
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("選擇 $label") },
            text = {
                Column {
                    // AndroidView NumberPicker (native wheel)
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = { ctx ->
                            // create the native NumberPicker instance
                            val np = android.widget.NumberPicker(ctx).apply {
                                minValue = range.first
                                maxValue = range.last
                                wrapSelectorWheel = true
                                // Do NOT assign 'value' here because 'value' parameter from composable shadows the property.
                            }
                            // Set the initial value and listener after creation
                            np.value = tempValue
                            np.setOnValueChangedListener { _, _, newVal ->
                                tempValue = newVal
                            }
                            np
                        },
                        update = { np ->
                            // keep the native picker in sync if tempValue changes
                            if (np.value != tempValue) np.value = tempValue
                        },
                        modifier = Modifier.height(200.dp)
                    )

                    Spacer(modifier = Modifier.padding(8.dp))

                    // show the selected value as confirmation UI inside the dialog
                    Text(
                        text = "目前: ${tempValue}",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onValueChange(tempValue.coerceIn(range.first, range.last))
                    showDialog = false
                }) {
                    Text("完成")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun TimerWithComplete(
    initialMinutes: Int = 0,
    initialSeconds: Int = 0,
    onComplete: () -> Unit = {},
    onFinish: () -> Unit = {} // additional callback for saving/navigating when timer finishes or user taps 完成
) {
    var minutes by remember { mutableStateOf(initialMinutes.coerceIn(0, 600)) }
    var seconds by remember { mutableStateOf(initialSeconds.coerceIn(0, 59)) }
    var running by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableStateOf(minutes * 60 + seconds) }

    // Keep remainingSeconds synced when user edits pickers
    LaunchedEffect(minutes, seconds) {
        if (!running) {
            remainingSeconds = minutes * 60 + seconds
        }
    }

    // Countdown when running
    LaunchedEffect(running) {
        while (running && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds -= 1
            minutes = remainingSeconds / 60
            seconds = remainingSeconds % 60
        }
        if (running && remainingSeconds == 0) {
            running = false
            onComplete()
            onFinish()
        }
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds), fontSize = 28.sp)
            Spacer(modifier = Modifier.padding(8.dp))
            Button(onClick = { running = !running }) {
                Text(if (running) "暫停" else "開始")
            }
            Spacer(modifier = Modifier.padding(8.dp))
            Button(onClick = {
                running = false
                onComplete()
                onFinish()
            }) {
                Text("完成")
            }
        }

        // Provide pickers to adjust minutes/seconds when not running
        if (!running) {
            Row {
                NumberPicker(value = minutes, onValueChange = {
                    minutes = it.coerceIn(0, 600)
                }, range = 0..600, label = "分", modifier = Modifier.weight(1f))

                Spacer(modifier = Modifier.padding(8.dp))

                NumberPicker(value = seconds, onValueChange = { seconds = it.coerceIn(0, 59) }, range = 0..59, label = "秒", modifier = Modifier.weight(1f))
            }
        }
    }
}
