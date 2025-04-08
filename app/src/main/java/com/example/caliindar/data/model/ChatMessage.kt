package com.example.caliindar.data.model

data class ChatMessage(
    val id: Long = System.currentTimeMillis(), // Простой ID для ключа в LazyColumn
    val text: String,
    val isUser: Boolean
)