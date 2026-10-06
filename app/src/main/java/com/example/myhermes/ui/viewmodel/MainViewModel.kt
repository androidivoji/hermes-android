package com.example.myhermes.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myhermes.data.model.ChatMessage
import com.example.myhermes.data.model.HermesModel
import com.example.myhermes.data.model.HermesModelDefaults
import com.example.myhermes.data.model.MessageSender
import com.example.myhermes.data.repository.HermesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val messages: List<ChatMessage> = emptyList(),
    val availableModels: List<HermesModel> = HermesModelDefaults.COMMON_MODELS,
    val selectedModel: HermesModel = HermesModelDefaults.DEFAULT_MODEL,
    val inputText: String = "",
    val attachedFileName: String? = null,
    val attachedImageBase64: String? = null,
    val isGenerating: Boolean = false,
    val isModelSheetVisible: Boolean = false,
    val activeChatTitle: String = "Percakapan Baru"
)

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: HermesRepository = HermesRepository()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadServerModels()
    }

    fun loadServerModels() {
        viewModelScope.launch {
            val serverModels = repository.fetchAvailableModels()
            if (serverModels.isNotEmpty()) {
                _uiState.update { state ->
                    state.copy(
                        availableModels = serverModels,
                        selectedModel = serverModels.first()
                    )
                }
            }
        }
    }

    fun addCustomModel(modelId: String) {
        val trimmed = modelId.trim()
        if (trimmed.isEmpty()) return

        val customModel = HermesModel(
            id = trimmed,
            name = trimmed,
            description = "Custom Model ID ($trimmed)",
            tag = "Custom"
        )

        _uiState.update { state ->
            val updatedList = (listOf(customModel) + state.availableModels).distinctBy { it.id }
            state.copy(
                availableModels = updatedList,
                selectedModel = customModel,
                isModelSheetVisible = false
            )
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun setAttachedImage(uri: Uri?, fileName: String?) {
        if (uri == null) {
            _uiState.update { it.copy(attachedFileName = null, attachedImageBase64 = null) }
            return
        }

        try {
            val context = getApplication<Application>()
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            if (bytes != null) {
                val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val dataUrl = "data:$mimeType;base64,$base64String"

                _uiState.update {
                    it.copy(
                        attachedFileName = fileName ?: "Gambar_Lampiran",
                        attachedImageBase64 = dataUrl
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    attachedFileName = fileName ?: "Lampiran",
                    attachedImageBase64 = null
                )
            }
        }
    }

    fun setSelectedModel(model: HermesModel) {
        _uiState.update {
            it.copy(
                selectedModel = model,
                isModelSheetVisible = false
            )
        }
    }

    fun setModelSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(isModelSheetVisible = visible) }
    }

    fun sendMessage(promptText: String? = null) {
        val currentText = _uiState.value.inputText.trim()
        val attachmentName = _uiState.value.attachedFileName
        val imageBase64 = _uiState.value.attachedImageBase64

        val textToSend = promptText ?: if (attachmentName != null) "$currentText\n[Lampiran Gambar: $attachmentName]" else currentText

        if (textToSend.isEmpty() && imageBase64 == null) return
        if (_uiState.value.isGenerating) return

        val finalPrompt = if (textToSend.isEmpty()) "Analisis gambar ini." else textToSend

        val userMessage = ChatMessage(
            sender = MessageSender.USER,
            text = finalPrompt
        )

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage,
                inputText = "",
                attachedFileName = null,
                attachedImageBase64 = null,
                isGenerating = true
            )
        }

        viewModelScope.launch {
            repository.sendPrompt(
                prompt = finalPrompt,
                selectedModel = _uiState.value.selectedModel,
                imageBase64 = imageBase64
            ).collect { hermesMessage ->
                _uiState.update { state ->
                    val existingMessages = state.messages.toMutableList()
                    val lastIndex = existingMessages.indexOfLast { it.id == hermesMessage.id }

                    if (lastIndex != -1) {
                        existingMessages[lastIndex] = hermesMessage
                    } else {
                        existingMessages.add(hermesMessage)
                    }

                    state.copy(
                        messages = existingMessages,
                        isGenerating = hermesMessage.isStreaming
                    )
                }
            }
        }
    }

    fun clearChat() {
        _uiState.update {
            it.copy(
                messages = emptyList(),
                attachedFileName = null,
                attachedImageBase64 = null,
                isGenerating = false
            )
        }
    }

    fun toggleLikeMessage(messageId: String, liked: Boolean) {
        _uiState.update { state ->
            val updated = state.messages.map { msg ->
                if (msg.id == messageId) {
                    msg.copy(isLiked = if (msg.isLiked == liked) null else liked)
                } else msg
            }
            state.copy(messages = updated)
        }
    }
}
