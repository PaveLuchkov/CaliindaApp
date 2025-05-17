package com.example.caliinda.data.auth


/*
@ExperimentalCoroutinesApi // Для использования тестовых диспетчеров и runTest
class AuthManagerTest {

    // Заменяем основной диспетчер на тестовый
    private val testDispatcher = StandardTestDispatcher()

    // --- Моки Зависимостей ---
    private lateinit var mockContext: Context
    private lateinit var mockOkHttpClient: OkHttpClient
    private lateinit var mockGoogleSignInClient: GoogleSignInClient
    private lateinit var mockCall: Call // Для мокания OkHttp вызова
    private lateinit var mockBackendUrl: String // Можно мокать или использовать константу
    private lateinit var mockWebClientId: String

    // --- Класс под Тестом ---
    private lateinit var authManager: AuthManager

    // Вспомогательные константы
    private val FAKE_BACKEND_URL = "http://fake.url"
    private val FAKE_WEB_CLIENT_ID = "fake-client-id"
    private val FAKE_ID_TOKEN = "fake-id-token"
    private val FAKE_AUTH_CODE = "fake-auth-code"
    private val FAKE_EMAIL = "test@example.com"

    @Before
    fun setUp() {
        // Устанавливаем тестовый диспетчер как основной
        Dispatchers.setMain(testDispatcher)

        // Инициализируем моки
        mockContext = mock()
        mockOkHttpClient = mock()
        mockGoogleSignInClient = mock()
        mockCall = mock() // Мок для OkHttp Call

        // Устанавливаем значения для аннотированных строк
        mockBackendUrl = FAKE_BACKEND_URL
        mockWebClientId = FAKE_WEB_CLIENT_ID

        // --- Важно: Мокаем GoogleSignIn.getClient ---
        // Так как AuthManager вызывает GoogleSignIn.getClient внутри себя,
        // нам нужно перехватить этот статический вызов. Это требует mockito-inline.
        // Если это сложно, можно передавать GoogleSignInClient как зависимость в конструктор AuthManager.
        // Будем считать, что GoogleSignInClient УЖЕ передается как зависимость (как мы делали)
        // или мокаем его создание, если оно внутри init AuthManager.
        // Для простоты предположим, что GoogleSignInClient внедряется или мокается напрямую.

        // Создаем экземпляр AuthManager с моками
        // Передаем тестовый диспетчер для предсказуемости
        authManager = AuthManager(
            context = mockContext,
            okHttpClient = mockOkHttpClient,
            backendBaseUrl = mockBackendUrl,
            webClientId = mockWebClientId
            // Если GoogleSignInClient создается внутри, нужно его мокать иначе.
            // Если он передается как зависимость:
            // googleSignInClient = mockGoogleSignInClient
        )

        // --- Мокаем вызов клиента по умолчанию (можно переопределить в тестах) ---
        // Делаем так, чтобы начальная проверка silentSignIn проваливалась по умолчанию
        whenever(authManager.googleSignInClient.silentSignIn())
            .thenReturn(Tasks.forException(ApiException(Status(GoogleSignInStatusCodes.SIGN_IN_REQUIRED))))
        whenever(authManager.googleSignInClient.signOut())
            .thenReturn(Tasks.forResult(null)) // Успешный выход по умолчанию
    }

    @After
    fun tearDown() {
        // Сбрасываем основной диспетчер после теста
        Dispatchers.resetMain()
    }

    // --- Тестовые Сценарии ---

    @Test
    fun `initial state is signed out and not loading`() = runTest(testDispatcher) {
        // Даем время на выполнение init блока и checkInitialAuthState
        advanceUntilIdle()

        val initialState = authManager.authState.value
        assertThat(initialState.isSignedIn).isFalse()
        assertThat(initialState.userEmail).isNull()
        assertThat(initialState.authError).isNull() // Ожидаем null, а не ошибку SIGN_IN_REQUIRED
        assertThat(initialState.isLoading).isFalse()
    }

    @Test
    fun `checkInitialAuthState when silent sign in succeeds updates state to signed in`() = runTest(testDispatcher) {
        // Arrange: Настроим mock silentSignIn на успех
        val mockAccount = mock<GoogleSignInAccount> {
            on { idToken } doReturn FAKE_ID_TOKEN
            on { email } doReturn FAKE_EMAIL
        }
        val successTask = Tasks.forResult(mockAccount)
        whenever(authManager.googleSignInClient.silentSignIn()).thenReturn(successTask)

        // Act: Пересоздаем AuthManager, чтобы вызвался init с новым моком silentSignIn
        authManager = AuthManager(mockContext, mockOkHttpClient, mockBackendUrl, mockWebClientId)
        advanceUntilIdle() // Даем корутине в init завершиться

        // Assert: Проверяем конечное состояние
        val finalState = authManager.authState.value
        assertThat(finalState.isSignedIn).isTrue()
        assertThat(finalState.userEmail).isEqualTo(FAKE_EMAIL)
        assertThat(finalState.authError).isNull()
        assertThat(finalState.isLoading).isFalse()

        // Можно использовать Turbine для проверки последовательности
        authManager.authState.test {
            // Пропускаем начальное состояние или проверяем его
            assertThat(awaitItem().isSignedIn).isFalse() // Начальное перед silentSignIn
            // Могут быть промежуточные isLoading=true
            val loadingState = awaitItem() // Ловим состояние загрузки
            assertThat(loadingState.isLoading).isTrue()
            // Конечное состояние
            val signedInState = awaitItem()
            assertThat(signedInState.isSignedIn).isTrue()
            assertThat(signedInState.userEmail).isEqualTo(FAKE_EMAIL)
            assertThat(signedInState.isLoading).isFalse() // Должно стать false

            cancelAndIgnoreRemainingEvents() // Завершаем тест Turbine
        }
    }

    @Test
    fun `handleSignInResult when google success and backend success updates state to signed in`() = runTest(testDispatcher) {
        // Arrange: Успешный вход Google
        val mockAccount = mock<GoogleSignInAccount> {
            on { idToken } doReturn FAKE_ID_TOKEN
            on { serverAuthCode } doReturn FAKE_AUTH_CODE
            on { email } doReturn FAKE_EMAIL
        }
        val googleSuccessTask: Task<GoogleSignInAccount> = Tasks.forResult(mockAccount)

        // Arrange: Успешный ответ бэкенда
        val mockResponseBody = "".toResponseBody("application/json".toMediaTypeOrNull())
        val mockResponse = mock<Response> {
            on { isSuccessful } doReturn true
            on { code } doReturn 200
            on { body } doReturn mockResponseBody
        }
        whenever(mockOkHttpClient.newCall(any())).thenReturn(mockCall) // Любой запрос вернет наш mock Call
        whenever(mockCall.execute()).thenReturn(mockResponse)

        // Act & Assert with Turbine
        authManager.authState.test {
            assertThat(awaitItem().isSignedIn).isFalse() // Начальное состояние

            authManager.handleSignInResult(googleSuccessTask) // Вызываем метод

            // Ожидаем состояние загрузки (отправка на бэкенд)
            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isTrue()
            assertThat(loadingState.userEmail).isEqualTo(FAKE_EMAIL) // Email может обновиться сразу

            // Ожидаем конечное состояние успеха
            val successState = awaitItem()
            assertThat(successState.isSignedIn).isTrue()
            assertThat(successState.userEmail).isEqualTo(FAKE_EMAIL)
            assertThat(successState.authError).isNull()
            assertThat(successState.isLoading).isFalse()

            // Verify: Проверяем, что был сделан запрос на бэкенд
            verify(mockOkHttpClient).newCall(argThat { request ->
                request.url.toString() == "$FAKE_BACKEND_URL/auth/google/exchange" && request.method == "POST"
            })
            verify(mockCall).execute()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `handleSignInResult when google success but backend fails updates state with error`() = runTest(testDispatcher) {
        // Arrange: Успешный вход Google
        val mockAccount = mock<GoogleSignInAccount> {
            on { idToken } doReturn FAKE_ID_TOKEN
            on { serverAuthCode } doReturn FAKE_AUTH_CODE
            on { email } doReturn FAKE_EMAIL
        }
        val googleSuccessTask: Task<GoogleSignInAccount> = Tasks.forResult(mockAccount)

        // Arrange: НЕуспешный ответ бэкенда
        val mockErrorBody = "{\"detail\":\"Backend Error\"}".toResponseBody("application/json".toMediaTypeOrNull())
        val mockResponse = mock<Response> {
            on { isSuccessful } doReturn false
            on { code } doReturn 500
            on { message } doReturn "Server Error"
            on { body } doReturn mockErrorBody // Возвращаем тело ошибки
        }
        whenever(mockOkHttpClient.newCall(any())).thenReturn(mockCall)
        whenever(mockCall.execute()).thenReturn(mockResponse)

        // Act & Assert with Turbine
        authManager.authState.test {
            assertThat(awaitItem().isSignedIn).isFalse() // Начальное

            authManager.handleSignInResult(googleSuccessTask)

            val loadingState = awaitItem() // Состояние загрузки
            assertThat(loadingState.isLoading).isTrue()
            assertThat(loadingState.userEmail).isEqualTo(FAKE_EMAIL)

            // Ожидаем состояние ошибки после ответа бэкенда
            val errorState = awaitItem()
            assertThat(errorState.isSignedIn).isFalse()
            assertThat(errorState.userEmail).isNull() // Сбрасывается при ошибке
            assertThat(errorState.authError).isEqualTo("Ошибка авторизации на сервере.") // Текст из signOutInternally
            assertThat(errorState.isLoading).isFalse()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `handleSignInResult when google fails updates state with google error`() = runTest(testDispatcher) {
        // Arrange: НЕуспешный вход Google
        val statusCode = GoogleSignInStatusCodes.NETWORK_ERROR
        val apiException = ApiException(Status(statusCode))
        val googleFailTask: Task<GoogleSignInAccount> = Tasks.forException(apiException)

        // Act & Assert with Turbine
        authManager.authState.test {
            assertThat(awaitItem().isSignedIn).isFalse() // Начальное

            authManager.handleSignInResult(googleFailTask)

            // Ожидаем состояние ошибки (isLoading может мелькнуть или нет)
            // Пропускаем промежуточные состояния, если они есть
            skipItems(count { it.isLoading }) // Пропускаем isLoading=true если оно было

            val errorState = awaitItem()
            assertThat(errorState.isSignedIn).isFalse()
            assertThat(errorState.userEmail).isNull()
            assertThat(errorState.authError).isEqualTo("Ошибка входа Google: $statusCode")
            assertThat(errorState.isLoading).isFalse()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getFreshIdToken when signed in and silent succeeds returns token`() = runTest(testDispatcher) {
        // Arrange: Сначала "войдем" пользователя
        val signInAccount = mock<GoogleSignInAccount> { on { idToken } doReturn FAKE_ID_TOKEN; on { email } doReturn FAKE_EMAIL }
        whenever(authManager.googleSignInClient.silentSignIn()).thenReturn(Tasks.forResult(signInAccount))
        // Выполняем вход (можно через handleSignInResult или установить состояние напрямую для простоты)
        authManager.authState.value = AuthState(isSignedIn = true, userEmail = FAKE_EMAIL) // Устанавливаем состояние напрямую
        advanceUntilIdle()

        // Arrange: Настроим следующий silentSignIn на успех с новым токеном
        val NEW_TOKEN = "new-fake-id-token"
        val refreshAccount = mock<GoogleSignInAccount> { on { idToken } doReturn NEW_TOKEN; on { email } doReturn FAKE_EMAIL }
        whenever(authManager.googleSignInClient.silentSignIn()).thenReturn(Tasks.forResult(refreshAccount))

        // Act
        val freshToken = authManager.getFreshIdToken()

        // Assert
        assertThat(freshToken).isEqualTo(NEW_TOKEN)
        // Проверяем, что состояние не изменилось на "вышел"
        assertThat(authManager.authState.value.isSignedIn).isTrue()
        assertThat(authManager.authState.value.authError).isNull()
    }

    @Test
    fun `getFreshIdToken when signed in but silent fails with SIGN_IN_REQUIRED signs out`() = runTest(testDispatcher) {
        // Arrange: "Входим" пользователя
        authManager.authState.value = AuthState(isSignedIn = true, userEmail = FAKE_EMAIL)
        advanceUntilIdle()

        // Arrange: Настроим silentSignIn на провал с SIGN_IN_REQUIRED
        val apiException = ApiException(Status(GoogleSignInStatusCodes.SIGN_IN_REQUIRED))
        whenever(authManager.googleSignInClient.silentSignIn()).thenReturn(Tasks.forException(apiException))

        // Act & Assert with Turbine
        authManager.authState.test {
            // Пропускаем начальное состояние (уже вошли)
            skipItems(1)

            // Act: Вызываем получение токена
            val token = authManager.getFreshIdToken()
            assertThat(token).isNull() // Ожидаем null

            // Assert: Ожидаем, что состояние изменится на "вышел" с ошибкой
            val errorState = awaitItem() // Ловим состояние ошибки сессии
            assertThat(errorState.authError).contains("Сессия истекла") // Или точное сообщение

            val finalState = awaitItem() // Ловим конечное состояние signedOut
            assertThat(finalState.isSignedIn).isFalse()
            assertThat(finalState.userEmail).isNull()
            assertThat(finalState.authError).contains("Сессия истекла") // Ошибка должна остаться
            assertThat(finalState.isLoading).isFalse()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `signOut when signed in updates state to signed out`() = runTest(testDispatcher) {
        // Arrange: "Входим" пользователя
        authManager.authState.value = AuthState(isSignedIn = true, userEmail = FAKE_EMAIL)
        advanceUntilIdle()
        // Мок для signOut уже настроен в setUp на успех

        // Act & Assert with Turbine
        authManager.authState.test {
            skipItems(1) // Пропускаем начальное "вошел"

            authManager.signOut() // Вызываем выход

            // Ожидаем isLoading = true
            assertThat(awaitItem().isLoading).isTrue()

            // Ожидаем конечное состояние "вышел"
            val finalState = awaitItem()
            assertThat(finalState.isSignedIn).isFalse()
            assertThat(finalState.userEmail).isNull()
            assertThat(finalState.authError).isEqualTo("Вы успешно вышли.") // Сообщение из signOutInternally
            assertThat(finalState.isLoading).isFalse()

            // Verify: Проверяем, что Google Sign Out был вызван
            verify(authManager.googleSignInClient).signOut()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearAuthError clears error in state`() = runTest(testDispatcher) {
        // Arrange: Установим состояние с ошибкой
        val error = "Some Auth Error"
        authManager.authState.value = AuthState(authError = error)
        advanceUntilIdle()
        assertThat(authManager.authState.value.authError).isEqualTo(error) // Убедимся, что установилось

        // Act
        authManager.clearAuthError()
        advanceUntilIdle() // Даем время на обновление StateFlow

        // Assert
        assertThat(authManager.authState.value.authError).isNull()
    }

    // Вспомогательная функция для удобного мокания OkHttp ответа
    private fun mockOkHttpResponse(code: Int, body: String?, isSuccessful: Boolean) {
        val responseBody = body?.toResponseBody("application/json".toMediaTypeOrNull())
        val mockResponse = mock<Response> {
            on { this.isSuccessful } doReturn isSuccessful
            on { this.code } doReturn code
            on { this.body } doReturn responseBody
            // Можем добавить on { close() } doNothing() если нужно
        }
        whenever(mockOkHttpClient.newCall(any())).thenReturn(mockCall)
        // Используем doReturn для suspend или обычной функции execute
        // Для простоты используем обычный execute, т.к. он вызывается из withContext(IO)
        whenever(mockCall.execute()).thenReturn(mockResponse)
    }
}

 */