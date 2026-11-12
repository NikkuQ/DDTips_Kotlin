package com.example.ddtips2

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.core.content.edit
import androidx.compose.foundation.layout.safeDrawingPadding

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MaterialTheme { DDTipsApp() } }
    }
}

data class TipsUiState(
    val tips: List<String> = emptyList(),
    val currentIndex: Int = 0,
    val timerEnabled: Boolean = true,
    val timerSeconds: Int = 7,
    val timerEpoch: Int = 0,
    val loaded: Boolean = false
) {
    val currentTip: String get() = tips.getOrElse(currentIndex) { "" }
}

class TipsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("ddtips", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(TipsUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val saved = preferences.getString("tips", null)
            val source = saved?.split(TIP_SEPARATOR)?.filter { it.isNotBlank() }
                ?: application.assets.open("D&D.txt").bufferedReader().use { it.readText() }
                    .split('|').map { it.trim() }.filter { it.isNotBlank() }
            val shuffled = source.shuffled()
            _state.value = TipsUiState(tips = shuffled, loaded = true)
        }
    }

    fun next() = move(1)
    fun previous() = move(-1)

    private fun move(delta: Int) {
        _state.update { old ->
            if (old.tips.isEmpty()) old else old.copy(
                currentIndex = (old.currentIndex + delta).floorMod(old.tips.size),
                timerEpoch = old.timerEpoch + 1
            )
        }
    }

    fun setTimer(enabled: Boolean) = _state.update { it.copy(timerEnabled = enabled, timerEpoch = it.timerEpoch + 1) }
    fun setTimerSeconds(seconds: Int) = _state.update { it.copy(timerSeconds = seconds, timerEpoch = it.timerEpoch + 1) }

    fun importTips(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val imported = getApplication<Application>().contentResolver.openInputStream(uri)
                ?.bufferedReader()?.use { it.readText() }
                ?.split('|')?.map { it.trim() }?.filter { it.isNotBlank() }.orEmpty()
            if (imported.isNotEmpty()) _state.update { it.copy(tips = imported.shuffled(), currentIndex = 0, timerEpoch = it.timerEpoch + 1) }
        }
    }

    fun saveTips(tips: List<String>) {
        val cleaned = tips.map { it.trim() }.filter { it.isNotBlank() }
        preferences.edit { putString("tips", cleaned.joinToString(TIP_SEPARATOR)) }
        _state.update { it.copy(tips = cleaned, currentIndex = 0, timerEpoch = it.timerEpoch + 1) }
    }

    private companion object { const val TIP_SEPARATOR = "\u001E" }
}

private fun Int.floorMod(modulus: Int): Int = ((this % modulus) + modulus) % modulus

@Composable
private fun DDTipsApp(viewModel: TipsViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    var editing by remember { mutableStateOf(false) }
    if (editing) TipsEditor(state.tips, onBack = { editing = false }, onSave = { viewModel.saveTips(it); editing = false })
    else HomeScreen(state, onPrevious = viewModel::previous, onNext = viewModel::next, onTimerEnabled = viewModel::setTimer, onTimerSeconds = viewModel::setTimerSeconds, onEdit = { editing = true }, onImport = viewModel::importTips)
}

@Composable
private fun HomeScreen(state: TipsUiState, onPrevious: () -> Unit, onNext: () -> Unit, onTimerEnabled: (Boolean) -> Unit, onTimerSeconds: (Int) -> Unit, onEdit: () -> Unit, onImport: (Uri) -> Unit) {
    val morrisRoman = FontFamily(Font(R.font.morris_roman_black))
    var optionsOpen by remember { mutableStateOf(false) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(onImport) }
    LaunchedEffect(state.timerEnabled, state.timerSeconds, state.timerEpoch, state.tips) {
        if (state.timerEnabled && state.tips.isNotEmpty()) { delay(state.timerSeconds * 1_000L); onNext() }
    }
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.bg), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton({ optionsOpen = true }, Modifier.alpha(.85f)) { Icon(Icons.Default.Settings, "Opzioni", tint = Color.White) }
                IconButton(onEdit, Modifier.alpha(.85f)) { Icon(Icons.Default.Add, "Modifica tip", tint = Color.White) }
            }
            Spacer(Modifier.weight(1f))
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Lo sapevi che:", fontSize = 34.sp, fontWeight = FontWeight.Bold, fontFamily = morrisRoman)
                    Spacer(Modifier.height(12.dp))
                    Text(if (state.loaded) state.currentTip else "Caricamento…", fontSize = 22.sp, fontFamily = morrisRoman)
                }
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.Center) {
                Button(onPrevious) { Text("←", fontSize = 28.sp) }
                Spacer(Modifier.width(30.dp))
                Button(onNext) { Text("→", fontSize = 28.sp) }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
    if (optionsOpen) {
        OptionsDialog(
            state = state,
            onDismiss = { optionsOpen = false },
            onTimerEnabled = onTimerEnabled,
            onTimerSeconds = onTimerSeconds,
            onImport = { importLauncher.launch(arrayOf("text/plain")) }
        )
    }
}

@Composable
private fun OptionsDialog(state: TipsUiState, onDismiss: () -> Unit, onTimerEnabled: (Boolean) -> Unit, onTimerSeconds: (Int) -> Unit, onImport: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Opzioni") }, text = {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Attiva/Disattiva timer", Modifier.weight(1f)); Switch(state.timerEnabled, onTimerEnabled) }
            Slider(value = state.timerSeconds.toFloat(), onValueChange = { onTimerSeconds(it.toInt().coerceIn(1, 30)) }, valueRange = 1f..30f, steps = 28, enabled = state.timerEnabled)
            Text("Tempo tra i tip: ${state.timerSeconds} secondi")
            Spacer(Modifier.height(18.dp))
            FilledTonalButton(onImport) { Icon(Icons.Default.FileOpen, null); Spacer(Modifier.width(8.dp)); Text("Importa file dei tip") }
        }
    }, confirmButton = { Button(onDismiss) { Text("Chiudi") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TipsEditor(initialTips: List<String>, onBack: () -> Unit, onSave: (List<String>) -> Unit) {
    var tips by remember(initialTips) { mutableStateOf(initialTips) }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        CenterAlignedTopAppBar(title = { Text("Lista dei TIP") }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro") } }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer))
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(10.dp)) {
            itemsIndexed(tips, key = { index, _ -> index }) { index, tip ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = tip, onValueChange = { value -> tips = tips.toMutableList().also { it[index] = value } }, modifier = Modifier.weight(1f), singleLine = false)
                    IconButton({ tips = tips.toMutableList().also { it.removeAt(index) } }) { Icon(Icons.Default.Delete, "Elimina", tint = MaterialTheme.colorScheme.error) }
                }
            }
        }
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            FilledTonalButton({ tips = tips + "" }) { Icon(Icons.Default.Add, null); Text(" Aggiungi") }
            Button({ onSave(tips) }) { Text("Salva") }
        }
    }
}

//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//private fun TipsEditor(
//    initialTips: List<String>,
//    onBack: () -> Unit,
//    onSave: (List<String>) -> Unit
//) {
//    var tips by remember(initialTips) {
//        mutableStateOf(initialTips.toMutableList())
//    }
//
//    Column(
//        modifier = Modifier.fillMaxSize()
//    ) {
//
//        CenterAlignedTopAppBar(
//            title = {
//                Text("Lista dei TIP")
//            },
//            navigationIcon = {
//                IconButton(
//                    onClick = onBack
//                ) {
//                    Icon(
//                        Icons.AutoMirrored.Filled.ArrowBack,
//                        contentDescription = "Indietro"
//                    )
//                }
//            },
//            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
//                containerColor = MaterialTheme.colorScheme.primaryContainer
//            )
//        )
//
//        LazyColumn(
//            modifier = Modifier.weight(1f),
//            contentPadding = PaddingValues(10.dp)
//        ) {
//
//            itemsIndexed(
//                tips,
//                key = { index, _ -> index }
//            ) { index, tip ->
//
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    modifier = Modifier.fillMaxWidth()
//                ) {
//
//                    OutlinedTextField(
//                        value = tip,
//                        onValueChange = { value ->
//                            tips = tips.toMutableList().apply {
//                                this[index] = value
//                            }
//                        },
//                        modifier = Modifier.weight(1f),
//                        singleLine = false
//                    )
//
//                    IconButton(
//                        onClick = {
//                            tips = tips.toMutableList().apply {
//                                removeAt(index)
//                            }
//                        }
//                    ) {
//                        Icon(
//                            Icons.Default.Delete,
//                            contentDescription = "Elimina",
//                            tint = MaterialTheme.colorScheme.error
//                        )
//                    }
//                }
//            }
//        }
//
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .background(
//                    MaterialTheme.colorScheme.primaryContainer
//                )
//                .padding(12.dp),
//            horizontalArrangement = Arrangement.SpaceBetween
//        ) {
//
//            FilledTonalButton(
//                onClick = {
//                    tips = tips.toMutableList().apply {
//                        add("")
//                    }
//                }
//            ) {
//                Icon(
//                    Icons.Default.Add,
//                    contentDescription = "Aggiungi"
//                )
//
//                Spacer(Modifier.width(8.dp))
//
//                Text("Aggiungi")
//            }
//
//            Button(
//                onClick = {
//                    onSave(tips)
//                }
//            ) {
//                Text("Salva")
//            }
//        }
//    }
//}
