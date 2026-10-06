package com.example.myhermes.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myhermes.ui.components.ChatMessageItem
import com.example.myhermes.ui.components.GeminiInputBar
import com.example.myhermes.ui.components.GeminiTopBar
import com.example.myhermes.ui.components.ModelSelectorSheet
import com.example.myhermes.ui.components.NavigationDrawerContent
import com.example.myhermes.ui.components.WelcomeSuggestions
import com.example.myhermes.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when messages list updates
    LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.text) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavigationDrawerContent(
                onNewChatClick = { viewModel.clearChat() },
                onClearChatClick = { viewModel.clearChat() },
                onCloseDrawer = {
                    coroutineScope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                GeminiTopBar(
                    selectedModel = uiState.selectedModel,
                    onMenuClick = {
                        coroutineScope.launch { drawerState.open() }
                    },
                    onModelSelectorClick = {
                        viewModel.setModelSheetVisible(true)
                    }
                )
            },
            bottomBar = {
                GeminiInputBar(
                    text = uiState.inputText,
                    onTextChanged = { viewModel.onInputTextChanged(it) },
                    onSendClick = { viewModel.sendMessage() },
                    attachedFileName = uiState.attachedFileName,
                    onImageSelected = { uri, name -> viewModel.setAttachedImage(uri, name) },
                    isGenerating = uiState.isGenerating
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (uiState.messages.isEmpty()) {
                    // Empty State: Gemini Welcome Screen & Quick Suggestions
                    WelcomeSuggestions(
                        onSuggestionClick = { prompt ->
                            viewModel.sendMessage(prompt)
                        }
                    )
                } else {
                    // Chat Messages Feed
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.messages,
                            key = { it.id }
                        ) { message ->
                            ChatMessageItem(
                                message = message,
                                onLikeToggle = { liked ->
                                    viewModel.toggleLikeMessage(message.id, liked)
                                },
                                onRegenerate = {
                                    viewModel.sendMessage(message.text)
                                }
                            )
                        }
                    }
                }
            }

            // Bottom Sheet for selecting Hermes Agent Model Variants
            if (uiState.isModelSheetVisible) {
                ModelSelectorSheet(
                    availableModels = uiState.availableModels,
                    selectedModel = uiState.selectedModel,
                    onModelSelected = { model ->
                        viewModel.setSelectedModel(model)
                    },
                    onAddCustomModel = { customId ->
                        viewModel.addCustomModel(customId)
                    },
                    onRefreshModels = {
                        viewModel.loadServerModels()
                    },
                    onDismiss = {
                        viewModel.setModelSheetVisible(false)
                    }
                )
            }
        }
    }
}
