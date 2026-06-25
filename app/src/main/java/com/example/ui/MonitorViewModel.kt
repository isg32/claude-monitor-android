package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ApiConfig
import com.example.data.UsageLog
import com.example.data.UsageRepository
import com.example.network.AnthropicClient
import com.example.network.MessageDto
import com.example.network.MessagesRequest
import com.example.network.ModelPricing
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class MonitorUiState(
    val logs: List<UsageLog> = emptyList(),
    val config: ApiConfig = ApiConfig(),
    val isDbLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val loginError: String? = null,
    val playgroundPrompt: String = "",
    val playgroundResponse: String = "",
    val isPlaygroundLoading: Boolean = false,
    val activeTab: Int = 0 // 0 = Dashboard, 1 = CLI Log, 2 = Playground, 3 = Settings
)

class MonitorViewModel(private val repository: UsageRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MonitorUiState())
    val uiState: StateFlow<MonitorUiState> = _uiState.asStateFlow()

    init {
        // Observe DB Logs and API Configuration
        viewModelScope.launch {
            repository.allUsageLogs.collect { logs ->
                _uiState.update { it.copy(logs = logs) }
                if (logs.isEmpty() && _uiState.value.isDbLoaded) {
                    preloadMockData()
                }
            }
        }

        viewModelScope.launch {
            repository.apiConfig.collect { config ->
                if (config != null) {
                    _uiState.update { it.copy(config = config, isDbLoaded = true) }
                } else {
                    // Initialize with default empty configuration (not logged in yet)
                    val defaultConfig = ApiConfig(id = 1, apiKey = "", monthlyBudget = 100.0, isDemoMode = false)
                    repository.saveApiConfig(defaultConfig)
                    _uiState.update { it.copy(config = defaultConfig, isDbLoaded = true) }
                }
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(activeTab = tabIndex, loginError = null) }
    }

    fun updatePlaygroundPrompt(prompt: String) {
        _uiState.update { it.copy(playgroundPrompt = prompt) }
    }

    fun updateBudget(budget: Double) {
        viewModelScope.launch {
            val current = _uiState.value.config
            repository.saveApiConfig(current.copy(monthlyBudget = budget))
        }
    }

    fun logInWithKey(apiKey: String, budget: Double, onSuccess: () -> Unit) {
        _uiState.update { it.copy(isLoading = true, loginError = null) }
        viewModelScope.launch {
            try {
                // Perform a lightweight validation request
                val testPrompt = MessagesRequest(
                    model = ModelPricing.CLAUDE_3_5_SONNET,
                    maxTokens = 10,
                    messages = listOf(MessageDto(role = "user", content = "Ping"))
                )

                // Try to hit Anthropic
                val response = AnthropicClient.apiService.createMessage(
                    apiKey = apiKey,
                    request = testPrompt
                )

                val inputTokens = response.usage.inputTokens
                val outputTokens = response.usage.outputTokens
                val cost = ModelPricing.calculateCost(ModelPricing.CLAUDE_3_5_SONNET, inputTokens, outputTokens)

                // If successful, save config and log this test as an actual run!
                val newConfig = ApiConfig(
                    id = 1,
                    apiKey = apiKey,
                    monthlyBudget = budget,
                    isDemoMode = false
                )
                repository.saveApiConfig(newConfig)

                val verificationLog = UsageLog(
                    commandName = "API Key Verification Ping",
                    timestamp = System.currentTimeMillis(),
                    modelName = ModelPricing.CLAUDE_3_5_SONNET,
                    inputTokens = inputTokens,
                    outputTokens = outputTokens,
                    cost = cost,
                    isPlaygroundQuery = false,
                    prompt = "Ping",
                    responseText = "Connection verified successfully!"
                )
                repository.insertUsageLog(verificationLog)

                _uiState.update { it.copy(isLoading = false, activeTab = 0) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loginError = "Failed to verify API Key: ${e.localizedMessage ?: "Invalid Key or Network Error"}"
                    )
                }
            }
        }
    }

    fun enableDemoMode() {
        viewModelScope.launch {
            val current = _uiState.value.config
            repository.saveApiConfig(current.copy(isDemoMode = true, apiKey = ""))
            _uiState.update { it.copy(loginError = null) }
            preloadMockData()
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.saveApiConfig(ApiConfig(id = 1, apiKey = "", monthlyBudget = 100.0, isDemoMode = false))
            _uiState.update { it.copy(activeTab = 0, loginError = null, playgroundResponse = "", playgroundPrompt = "") }
        }
    }

    fun refreshData() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            delay(1200) // Simulated synchronization delay
            val config = _uiState.value.config
            if (config.isDemoMode) {
                // Generate simulated Claude Code developer logs
                val mockCommands = listOf(
                    "claude \"verify compilation and run roborazzi tests\"",
                    "claude \"refactor details panel and add rich tooltips\"",
                    "claude \"implement dynamic color scheme in bento grids\"",
                    "claude \"write unit test suite for billing controller\"",
                    "claude \"optimize SQL database queries with SQLite indexers\""
                )
                val mockModels = listOf(ModelPricing.CLAUDE_3_5_SONNET, ModelPricing.CLAUDE_3_5_HAIKU)
                val command = mockCommands.random()
                val model = mockModels.random()
                val inputTokens = (4000..22000).random()
                val outputTokens = (500..3800).random()
                val cost = ModelPricing.calculateCost(model, inputTokens, outputTokens)

                val log = UsageLog(
                    commandName = command,
                    timestamp = System.currentTimeMillis(),
                    modelName = model,
                    inputTokens = inputTokens,
                    outputTokens = outputTokens,
                    cost = cost,
                    isPlaygroundQuery = false
                )
                repository.insertUsageLog(log)
            }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearUsageLogs()
        }
    }

    fun submitPlaygroundQuery() {
        val prompt = _uiState.value.playgroundPrompt.trim()
        if (prompt.isEmpty()) return

        _uiState.update { it.copy(isPlaygroundLoading = true, playgroundResponse = "") }

        viewModelScope.launch {
            val config = _uiState.value.config
            if (config.isDemoMode) {
                // Simulate Claude's response after a realistic delay
                delay(1500)
                val mockResponses = listOf(
                    "Here's a standard Kotlin function for networking using Ktor:\n\n```kotlin\nval client = HttpClient(CIO)\nsuspend fun fetchStats(): String = client.get(\"https://api.anthropic.com/v1/stats\").body()\n```",
                    "To optimize your Claude Code queries, try to minimize the workspace context. You can do this by using a `.claudecodeignore` file to prevent reading large asset files or node folders.",
                    "Yes, Room database uses SQLite underneath. By returning Flow<List<T>> from your DAO, Room notifies Jetpack Compose of any write changes automatically, keeping your UI reactive.",
                    "Here is a CSS grid solution for dynamic card heights:\n\n```css\n.grid {\n  display: grid;\n  grid-template-columns: repeat(auto-fit, minmax(280dp, 1fr));\n  gap: 16px;\n}\n```",
                    "The `claude-3-5-sonnet` model is Anthropic's state-of-the-art model for coding. It costs \$3 per million input tokens and \$15 per million output tokens, which makes it highly cost-effective for large refactors."
                )
                val responseText = mockResponses.random()
                val mockInputTokens = (300..1200).random()
                val mockOutputTokens = (150..500).random()
                val mockModel = ModelPricing.CLAUDE_3_5_SONNET
                val cost = ModelPricing.calculateCost(mockModel, mockInputTokens, mockOutputTokens)

                val newLog = UsageLog(
                    commandName = "Playground Query",
                    timestamp = System.currentTimeMillis(),
                    modelName = mockModel,
                    inputTokens = mockInputTokens,
                    outputTokens = mockOutputTokens,
                    cost = cost,
                    isPlaygroundQuery = true,
                    prompt = prompt,
                    responseText = responseText
                )

                repository.insertUsageLog(newLog)
                _uiState.update {
                    it.copy(
                        isPlaygroundLoading = false,
                        playgroundResponse = responseText,
                        playgroundPrompt = ""
                    )
                }
            } else {
                // Live Mode: query real Anthropic endpoint
                try {
                    val messagesRequest = MessagesRequest(
                        model = ModelPricing.CLAUDE_3_5_SONNET,
                        maxTokens = 1024,
                        messages = listOf(MessageDto(role = "user", content = prompt))
                    )

                    val response = AnthropicClient.apiService.createMessage(
                        apiKey = config.apiKey,
                        request = messagesRequest
                    )

                    val contentText = response.content.firstOrNull { it.type == "text" }?.text ?: "No text content received."
                    val inputTokens = response.usage.inputTokens
                    val outputTokens = response.usage.outputTokens
                    val cost = ModelPricing.calculateCost(ModelPricing.CLAUDE_3_5_SONNET, inputTokens, outputTokens)

                    val newLog = UsageLog(
                        commandName = "Playground: " + (if (prompt.length > 30) prompt.take(30) + "..." else prompt),
                        timestamp = System.currentTimeMillis(),
                        modelName = ModelPricing.CLAUDE_3_5_SONNET,
                        inputTokens = inputTokens,
                        outputTokens = outputTokens,
                        cost = cost,
                        isPlaygroundQuery = true,
                        prompt = prompt,
                        responseText = contentText
                    )

                    repository.insertUsageLog(newLog)
                    _uiState.update {
                        it.copy(
                            isPlaygroundLoading = false,
                            playgroundResponse = contentText,
                            playgroundPrompt = ""
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isPlaygroundLoading = false,
                            playgroundResponse = "Error sending message: ${e.localizedMessage ?: "Unknown network error."}"
                        )
                    }
                }
            }
        }
    }

    private suspend fun preloadMockData() {
        val now = System.currentTimeMillis()
        val mockLogs = listOf(
            UsageLog(
                commandName = "claude \"init new android project with jetpack compose\"",
                timestamp = now - 2 * 60 * 60 * 1000, // 2 hours ago
                modelName = ModelPricing.CLAUDE_3_5_SONNET,
                inputTokens = 12450,
                outputTokens = 1820,
                cost = ModelPricing.calculateCost(ModelPricing.CLAUDE_3_5_SONNET, 12450, 1820),
                isPlaygroundQuery = false
            ),
            UsageLog(
                commandName = "claude \"fix compilation error in MainActivity.kt\"",
                timestamp = now - 6 * 60 * 60 * 1000, // 6 hours ago
                modelName = ModelPricing.CLAUDE_3_5_HAIKU,
                inputTokens = 4200,
                outputTokens = 480,
                cost = ModelPricing.calculateCost(ModelPricing.CLAUDE_3_5_HAIKU, 4200, 480),
                isPlaygroundQuery = false
            ),
            UsageLog(
                commandName = "claude \"generate a beautiful custom canvas line chart for dashboard\"",
                timestamp = now - 24 * 60 * 60 * 1000, // 1 day ago
                modelName = ModelPricing.CLAUDE_3_5_SONNET,
                inputTokens = 15300,
                outputTokens = 2300,
                cost = ModelPricing.calculateCost(ModelPricing.CLAUDE_3_5_SONNET, 15300, 2300),
                isPlaygroundQuery = false
            ),
            UsageLog(
                commandName = "claude \"run robolectric and roborazzi screenshot tests\"",
                timestamp = now - 36 * 60 * 60 * 1000, // 1.5 days ago
                modelName = ModelPricing.CLAUDE_3_5_HAIKU,
                inputTokens = 8500,
                outputTokens = 920,
                cost = ModelPricing.calculateCost(ModelPricing.CLAUDE_3_5_HAIKU, 8500, 920),
                isPlaygroundQuery = false
            ),
            UsageLog(
                commandName = "claude \"create Room database schema and DAO entities\"",
                timestamp = now - 2 * 24 * 60 * 60 * 1000, // 2 days ago
                modelName = ModelPricing.CLAUDE_3_5_SONNET,
                inputTokens = 21400,
                outputTokens = 3100,
                cost = ModelPricing.calculateCost(ModelPricing.CLAUDE_3_5_SONNET, 21400, 3100),
                isPlaygroundQuery = false
            ),
            UsageLog(
                commandName = "claude \"refactor package structure for clean architecture\"",
                timestamp = now - 3 * 24 * 60 * 60 * 1000, // 3 days ago
                modelName = ModelPricing.CLAUDE_3_OPUS,
                inputTokens = 32000,
                outputTokens = 4500,
                cost = ModelPricing.calculateCost(ModelPricing.CLAUDE_3_OPUS, 32000, 4500),
                isPlaygroundQuery = false
            ),
            UsageLog(
                commandName = "claude \"optimize gradle and compress background images\"",
                timestamp = now - 4 * 24 * 60 * 60 * 1000, // 4 days ago
                modelName = ModelPricing.CLAUDE_3_5_HAIKU,
                inputTokens = 6100,
                outputTokens = 550,
                cost = ModelPricing.calculateCost(ModelPricing.CLAUDE_3_5_HAIKU, 6100, 550),
                isPlaygroundQuery = false
            ),
            UsageLog(
                commandName = "claude \"add auth screen and biometric validation flows\"",
                timestamp = now - 5 * 24 * 60 * 60 * 1000, // 5 days ago
                modelName = ModelPricing.CLAUDE_3_5_SONNET,
                inputTokens = 18900,
                outputTokens = 2100,
                cost = ModelPricing.calculateCost(ModelPricing.CLAUDE_3_5_SONNET, 18900, 2100),
                isPlaygroundQuery = false
            ),
            UsageLog(
                commandName = "claude \"integrate Retrofit networking services and Moshi adapter\"",
                timestamp = now - 6 * 24 * 60 * 60 * 1000, // 6 days ago
                modelName = ModelPricing.CLAUDE_3_HAIKU,
                inputTokens = 11200,
                outputTokens = 1450,
                cost = ModelPricing.calculateCost(ModelPricing.CLAUDE_3_HAIKU, 11200, 1450),
                isPlaygroundQuery = false
            ),
            UsageLog(
                commandName = "claude \"configure production ProGuard and R8 rules\"",
                timestamp = now - 7 * 24 * 60 * 60 * 1000, // 7 days ago
                modelName = ModelPricing.CLAUDE_3_5_HAIKU,
                inputTokens = 5200,
                outputTokens = 410,
                cost = ModelPricing.calculateCost(ModelPricing.CLAUDE_3_5_HAIKU, 5200, 410),
                isPlaygroundQuery = false
            )
        )

        for (log in mockLogs) {
            repository.insertUsageLog(log)
        }
    }
}

class MonitorViewModelFactory(private val repository: UsageRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MonitorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MonitorViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
