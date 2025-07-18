package com.lpavs.caliinda.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lpavs.caliinda.R
import com.lpavs.caliinda.ui.screens.main.MainViewModel
import com.lpavs.caliinda.ui.screens.main.components.UIDefaults.cuid
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
) {
  val snackbarHostState = remember { SnackbarHostState() }
  val currentTemper by viewModel.botTemperState.collectAsStateWithLifecycle()
  var temperInputState by remember(currentTemper) { mutableStateOf(currentTemper) }
  val keyboardController = LocalSoftwareKeyboardController.current
  val scope = rememberCoroutineScope()

  Scaffold(
      snackbarHost = { SnackbarHost(snackbarHostState) },
      topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.aisettings)) },
            navigationIcon = {
              IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back))
              }
            })
      }) { paddingValues ->
        Column(
            modifier =
                Modifier.padding(paddingValues)
                    .padding(16.dp) // Дополнительные отступы для контента
                    .fillMaxWidth() // Занимаем всю ширину
            ) {
              Box(
                  modifier =
                      Modifier.fillMaxWidth()
                          .clip(RoundedCornerShape(cuid.SettingsItemCornerRadius))
                          .background(color = colorScheme.surfaceContainer)
                          .padding(16.dp)) {
                    Column {
                      Text(stringResource(R.string.temper_ai), style = typography.titleMedium)
                      Spacer(modifier = Modifier.height(8.dp))
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = temperInputState,
                            onValueChange = { temperInputState = it },
                            placeholder = { Text(stringResource(R.string.temper_example)) },
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            maxLines = 5,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions =
                                KeyboardActions(onDone = { keyboardController?.hide() }),
                            shape = RoundedCornerShape(cuid.SettingsItemCornerRadius))
                        Button(
                            onClick = {
                              if (temperInputState != currentTemper) {
                                viewModel.updateBotTemperSetting(temperInputState)
                                scope.launch {
                                  snackbarHostState.showSnackbar(R.string.temper_saved.toString())
                                }
                              }
                            },
                            enabled = temperInputState != currentTemper,
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp),
                            contentPadding = PaddingValues(0.dp)) {
                              Icon(
                                  imageVector = Icons.Rounded.Check,
                                  contentDescription = stringResource(R.string.save),
                                  modifier = Modifier.size(24.dp) // размер иконки внутри
                                  )
                            }
                      }
                    }
                  }
            }
      }
}
