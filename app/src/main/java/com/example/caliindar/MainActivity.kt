package com.example.caliindar

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels // Для by viewModels()
import androidx.compose.foundation.layout.*
import androidx.compose.material3.* // Используем Material 3
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle // Рекомендуемый способ сбора StateFlow
import com.example.caliindar.ui.theme.CaliindarTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.material.icons.Icons
import androidx.compose.material3.* // Используем Material 3
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Mic
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.activity.result.ActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.draw.clip
import androidx.compose.material3.OutlinedTextFieldDefaults.contentPadding
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler // Для открытия ссылок
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.TopAppBarDefaults.windowInsets
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.android.gms.common.api.Status
import com.google.android.gms.tasks.Tasks
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.min
import kotlin.text.toFloat
import kotlin.times
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.navigation.compose.rememberNavController


class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    // Лаунчер остается здесь!
    private lateinit var googleSignInLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            CaliindarTheme {
                // Создаем лаунчер ВНУТРИ @Composable контекста
                googleSignInLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult(),
                    onResult = { result ->
                        // Логика обработки результата остается здесь
                        if (result.resultCode == RESULT_OK) {
                            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                            viewModel.handleSignInResult(task)
                        } else { /* ... обработка ошибки ... */ }
                    }
                )

                // --- Навигация ---
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "main") {
                    composable("main") {
                        MainScreen(
                            viewModel = viewModel,
                            onNavigateToSettings = {
                                navController.navigate("settings") // Переход на экран настроек
                            }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            viewModel = viewModel, // Получаем ViewModel (возможно, через hiltViewModel())
                            // Передаем лямбду для запуска входа
                            onSignInClick = {
                                val signInIntent = viewModel.getSignInIntent()
                                if (signInIntent != null) {
                                    googleSignInLauncher.launch(signInIntent) // Лаунчер вызывается здесь
                                } else {
                                    // Можно показать Snackbar или обработать ошибку иначе
                                    Log.e("MainActivity", "Failed to get sign-in intent for launcher.")
                                    // scope.launch { snackbarHostState.showSnackbar("...")} // Snackbar нужен будет в SettingsScreen
                                }
                            },
                            onNavigateBack = { navController.popBackStack() } // Лямбда для кнопки "Назад"
                        )
                    }
                }
            }
        }
    }
}

// --- Главный Composable экрана ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, onNavigateToSettings: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var textFieldState by remember { mutableStateOf(TextFieldValue("")) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val uriHandler = LocalUriHandler.current
    val snackbarHostState = remember { SnackbarHostState() }

    // --- State for new bottom bar ---
    var isTextInputVisible by remember { mutableStateOf(false) }

    // --- Google Sign-In Launcher (Keep as is) ---
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = StartActivityForResult(),
        onResult = { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                viewModel.handleSignInResult(task)
            } else {
                Log.w("MainScreen", "Google Sign-In failed or cancelled. ResultCode: ${result.resultCode}")
                viewModel.handleSignInResult(
                    Tasks.forException(
                        ApiException(
                            Status(
                                result.resultCode,
                                "Google Sign-In flow was cancelled or failed (Result code: ${result.resultCode})."
                            )
                        )
                    )
                )
            }
        }
    )

    // --- Side Effects (Snackbar) ---
    LaunchedEffect(uiState.showGeneralError) {
        uiState.showGeneralError?.let { error ->
            snackbarHostState.showSnackbar("Ошибка: $error")
            viewModel.clearGeneralError()
        }
    }
    LaunchedEffect(uiState.showAuthError) {
        uiState.showAuthError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearAuthError()
        }
    }

    // Scroll chat to bottom when new message arrives
    LaunchedEffect(uiState.chatHistory.size) {
        if (uiState.chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(uiState.chatHistory.size - 1)
        }
    }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Caliinda") },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (uiState.isLoading && !uiState.isRecording) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 8.dp)
                            )
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Настройки"
                            )
                        }
                    }
                }
            )

        },
        bottomBar = {
            ChatInputBar(
                uiState = uiState,
                viewModel = viewModel,
                textFieldValue = textFieldState,
                onTextChanged = { textFieldState = it },
                onSendClick = {
                    viewModel.sendTextMessage(textFieldState.text)
                    textFieldState = TextFieldValue("")
                },
                isTextInputVisible = isTextInputVisible,
                onToggleTextInput = { isTextInputVisible = !isTextInputVisible }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(
                items = uiState.chatHistory,
                key = { message -> message.id }
            ) { message ->
                ChatMessageBubble(message = message, uriHandler = uriHandler)
            }
        }
    }
}

// --- New Bottom Bar Composable ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInputBar(
    uiState: MainUiState,
    viewModel: MainViewModel,
    textFieldValue: TextFieldValue,
    onTextChanged: (TextFieldValue) -> Unit,
    onSendClick: () -> Unit,
    isTextInputVisible: Boolean,
    onToggleTextInput: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val isSendEnabled = textFieldValue.text.isNotBlank() && uiState.isSignedIn && !uiState.isLoading && !uiState.isRecording
    val isRecordEnabled = uiState.isSignedIn && !uiState.isLoading // Keep record enabled even if text input is visible
    val isKeyboardToggleEnabled = uiState.isSignedIn && !uiState.isLoading && !uiState.isRecording // Disable toggle during recording/loading

    // Request focus when text input becomes visible
    LaunchedEffect(isTextInputVisible) {
        if (isTextInputVisible) {
            // kotlinx.coroutines.delay(100) // Small delay might be needed
            try {
                focusRequester.requestFocus()
                keyboardController?.show()
                Log.d("ChatInputBar", "Focus requested and keyboard show attempted.")
            } catch (e: Exception) {
                Log.e("ChatInputBar", "Error requesting focus or showing keyboard", e)
            }
        } else {
            keyboardController?.hide()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // --- Text Input Row (Conditionally Visible) ---
        AnimatedVisibility(
            visible = isTextInputVisible,
            enter = slideInVertically(initialOffsetY = { it }), // Slide in from bottom
            exit = slideOutVertically(targetOffsetY = { it }) // Slide out to bottom
        ) {
            Surface(tonalElevation = 4.dp) { // Add elevation similar to input fields
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textFieldValue,
                        onValueChange = onTextChanged,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester), // Apply focus requester
                        shape = RoundedCornerShape(24.dp), // More rounded
                        placeholder = { Text("Сообщение...") },
                        enabled = uiState.isSignedIn && !uiState.isLoading && !uiState.isRecording,
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors( // Optional: Customize colors
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Send Button (IconButton)
                    IconButton(
                        onClick = onSendClick,
                        enabled = isSendEnabled,
                        modifier = Modifier.size(48.dp), // Consistent size
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isSendEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            contentColor = if (isSendEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Отправить сообщение"
                        )
                    }
                }
            }
        }

        // --- Bottom App Bar with Icons ---
        BottomAppBar(
            actions = {
                // Keyboard Toggle Button
                IconButton(
                    onClick = onToggleTextInput,
                    enabled = isKeyboardToggleEnabled
                ) {
                    Icon(
                        imageVector = Icons.Filled.Keyboard,
                        contentDescription = if (isTextInputVisible) "Скрыть клавиатуру" else "Показать клавиатуру"
                    )
                }
                // You could add other actions here if needed
            },
            floatingActionButton = {
                RecordButton( // Use the existing RecordButton
                    uiState = uiState,
                    viewModel = viewModel,
                    modifier = Modifier.size(56.dp) // Standard FAB size
                    // Note: RecordButton's internal pointerInput handles enable state based on its own logic + uiState
                )
            }
        )
    }
}

@Composable
fun RecordButton(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier // Принимаем внешний модификатор
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // --- Состояние для отслеживания нажатия для UI ---
    var isPressed by remember { mutableStateOf(false) }

    // --- Анимация цвета ---
    val targetBackgroundColor = if (uiState.isRecording) {
        MaterialTheme.colorScheme.error // Красный при записи
    } else {
        MaterialTheme.colorScheme.primary // Обычный цвет
    }
    val animatedBackgroundColor by animateColorAsState(
        targetValue = targetBackgroundColor,
        animationSpec = tween(durationMillis = 300),
        label = "RecordButtonBackgroundColor"
    )
    val animatedContentColor = contentColorFor(animatedBackgroundColor)

    // --- Определение форм для морфинга ---
    val shapeA = remember { // Начальная форма (можно сделать ближе к кругу)
        RoundedPolygon(
            // numVertices = 64, // Много вершин = почти круг
            numVertices = 5, // Как в примере
            rounding = CornerRounding(0.5f) // Скругление
        )
    }
    val shapeB = remember {
        RoundedPolygon.star(
            9,
            rounding = CornerRounding(0.3f),
            radius = 4f
        )
    }
    val morph = remember {
        Morph(shapeA, shapeB)
    }

    // --- Анимация вращения ---
    val infiniteTransition = rememberInfiniteTransition("morph_transition")
    val animatedProgress = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1800, easing = LinearEasing), // Скорость морфинга
            repeatMode = RepeatMode.Reverse
        ),
        label = "animatedMorphProgress"
    )
    val animatedRotation = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(4000, easing = LinearEasing), // Скорость вращения
            repeatMode = RepeatMode.Restart // Постоянное вращение в одну сторону
            // repeatMode = RepeatMode.Reverse // Вращение туда-обратно, как в примере
        ),
        label = "animatedMorphRotation"
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed || uiState.isRecording) 1.45f else 1.0f, // Увеличиваем на 40%
        animationSpec = tween(durationMillis = 300),
        label = "RecordButtonScale"
    )

    // Лаунчер для запроса разрешения (без изменений)
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        viewModel.updatePermissionStatus(isGranted)
        if (!isGranted) {
            Toast.makeText(context, "Разрешение на запись отклонено.", Toast.LENGTH_LONG).show()
        } else {
            Log.i("RecordButton", "Permission granted by user.")
            // Можно подсказать, что нужно нажать еще раз
            Toast.makeText(
                context,
                "Разрешение получено. Нажмите и удерживайте для записи.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Кнопка активна, если пользователь вошел, не идет загрузка
    val isInteractionEnabled = uiState.isSignedIn && !uiState.isLoading
    Log.d(
        "RecordButton",
        "Rendering FAB: isInteractionEnabled=$isInteractionEnabled, isPressed=$isPressed, isRecording=${uiState.isRecording}"
    )

        FloatingActionButton(
            onClick = { Log.d("RecordButton", "FAB onClick triggered (ignored)") },
            containerColor = animatedBackgroundColor,
            contentColor = animatedContentColor,
            modifier = modifier
                .size(56.dp)
                .pointerInput(isInteractionEnabled) { // Логика нажатия/отпускания (без изменений)
                    if (!isInteractionEnabled) {return@pointerInput}
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            isPressed = true
                            try {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                viewModel.updatePermissionStatus(hasPermission)
                                if (hasPermission) {
                                    down.consume()
                                    scope.launch { viewModel.startRecording() }
                                    try {
                                        waitForUpOrCancellation()
                                    } finally {
                                        scope.launch { viewModel.stopRecordingAndSend() }
                                    }
                                } else {
                                    down.consume()
                                    scope.launch { requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                                    try {
                                        waitForUpOrCancellation()
                                    } finally { /* Ничего не делаем */
                                    }
                                }
                            } finally {
                                isPressed = false
                            }
                        }
                    }
                }
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                    // Вращение можно оставить здесь ИЛИ положиться на вращение в CustomRotatingMorphShape
                    // Если вращение в Shape уже есть, здесь его можно убрать:
                    // rotationZ = if (isPressed || uiState.isRecording) animatedRotation.value else 0f
                }
                .clip(
                    if (isPressed || uiState.isRecording) {
                        CustomRotatingMorphShape(
                            morph = morph,
                            percentage = animatedProgress.value,
                            rotation = animatedRotation.value // Используем вращение из Shape
                            // rotation = 0f // Если вращение делается в graphicsLayer выше
                        )
                    } else {
                        RoundedCornerShape(16.dp) // Стандартная форма в покое
                    }
                ),
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = if (uiState.isRecording) "Идет запись (Отпустите для остановки)" else "Начать запись (Нажмите и удерживайте)",
                tint = animatedContentColor
            )
        }
    }

// --- ChatMessageBubble (Keep as is) ---
@Composable
fun ChatMessageBubble(message: ChatMessage, uriHandler: UriHandler) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.8f) // Занимает не всю ширину
                .clip(RoundedCornerShape(16.dp)),
            color = if (message.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
            tonalElevation = 1.dp
        ) {
            ClickableLinkText( // Используем новый Composable для ссылок
                text = message.text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                uriHandler = uriHandler
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onSignInClick: () -> Unit, // <-- Принимаем лямбду вместо лаунчера
    onNavigateBack: () -> Unit // <-- Лямбда для возврата назад
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = { // Кнопка "Назад"
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            if (!uiState.isSignedIn) {
                Button(onClick = onSignInClick) { // <-- Вызываем лямбду
                    Text("Войти через Google")
                }
            } else {
                Text("Вы вошли как: ${uiState.userEmail ?: "..."}")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { viewModel.signOut() }) { // Выход остается через ViewModel
                    Text("Выйти")
                }
            }
            // ... остальное (выбор таймзоны) ...
        }
    }
}

// --- ClickableLinkText (Keep as is) ---
@Composable
fun ClickableLinkText(
    text: String,
    modifier: Modifier = Modifier,
    uriHandler: UriHandler = LocalUriHandler.current
) {
    val linkRegex = remember { Regex("\\[([^]]+)]\\(([^)]+)\\)") } // [Text](URL)
    val annotatedString = remember(text) {
        buildAnnotatedString {
            var lastIndex = 0
            linkRegex.findAll(text).forEach { matchResult ->
                val linkText = matchResult.groupValues[1]
                val url = matchResult.groupValues[2]
                val startIndex = matchResult.range.first
                val endIndex = matchResult.range.last + 1

                append(text.substring(lastIndex, startIndex))
                pushStringAnnotation(tag = "URL", annotation = url)
                withStyle(style = SpanStyle(color = Color(0xFF64B5F6), textDecoration = TextDecoration.Underline)) {
                    append(linkText)
                }
                pop()
                lastIndex = endIndex
            }
            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }
    }

    ClickableText( // Use Material 3 ClickableText for better integration
        text = annotatedString,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium.copy(color = LocalContentColor.current), // Inherit color
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    try {
                        uriHandler.openUri(annotation.item)
                    } catch (e: Exception) {
                        Log.e("ClickableLinkText", "Failed to open URI ${annotation.item}", e)
                    }
                }
        }
    )
}

// --- showToast Helper (Keep as is) ---
private fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}

// --- Preview для Android Studio ---
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CaliindarTheme {
        // НЕ СОЗДАЕМ ЗДЕСЬ НАСТОЯЩИЙ ViewModel!
        // Вызываем Composable с дефолтным состоянием или моковыми данными.
        PreviewScreenContent() // Использует uiState по умолчанию из своей сигнатуры
    }
}



@Composable
fun PreviewScreenContent(
    uiState: MainUiState = MainUiState(), // Дефолтное состояние для превью
    onRecordClick: () -> Unit = {},
    onSignInClick: () -> Unit = {},
    onSignOutClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = uiState.message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
            }

            Button(
                onClick = onRecordClick,
                enabled = uiState.isSignedIn && !uiState.isLoading,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (!uiState.isSignedIn) {
                Button(
                    onClick = onSignInClick,
                    enabled = !uiState.isLoading,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text("Войти через Google")
                }
            }

            if (uiState.isSignedIn) {
                Button(
                    onClick = onSignOutClick,
                    enabled = !uiState.isLoading
                ) {
                    Text("Выйти из аккаунта")
                }
                uiState.userEmail?.let { email ->
                    Text(
                        text = "Вошли как: $email",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

// Пример превью с разными состояниями
@Preview(showBackground = true, name = "Signed In State")
@Composable
fun SignedInPreview() {
    CaliindarTheme {
        PreviewScreenContent(
            uiState = MainUiState(
                message = "Аккаунт: test@example.com (Авторизован)",
                isSignedIn = true,
                userEmail = "test@example.com",
                isPermissionGranted = true
            )
        )
    }
}

@Preview(showBackground = true, name = "Signed Out State")
@Composable
fun SignedOutPreview() {
    CaliindarTheme {
        PreviewScreenContent(
            uiState = MainUiState(
                message = "Требуется вход и авторизация",
                isSignedIn = false
            )
        )
    }
}

@Preview(showBackground = true, name = "Recording State")
@Composable
fun RecordingPreview() {
    CaliindarTheme {
        PreviewScreenContent(
            uiState = MainUiState(
                message = "Запись началась...",
                isSignedIn = true,
                userEmail = "test@example.com",
                isPermissionGranted = true,
                isRecording = true
            )
        )
    }
}

class CustomRotatingMorphShape(
    private val morph: Morph,
    private val percentage: Float,
    private val rotation: Float
) : Shape {

    private val matrix = Matrix()
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        // Растягиваем на размер контейнера
        matrix.reset() // Сбрасываем матрицу перед использованием
        matrix.scale(size.width / 2f, size.height / 2f)
        matrix.translate(1f, 1f) // Центрируем (предполагается радиус 1f в Morph)
        matrix.rotateZ(rotation) // Вращаем

        // Получаем путь из Morph и трансформируем
        val path = morph.toPath(progress = percentage).asComposePath()
        path.transform(matrix)

        return Outline.Generic(path)
    }
}