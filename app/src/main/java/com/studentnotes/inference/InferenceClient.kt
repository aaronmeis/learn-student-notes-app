package com.studentnotes.inference

interface InferenceClient {
    suspend fun generateFlashcards(noteContent: String): List<FlashcardResult>
    suspend fun answerQuestion(noteContent: String, question: String): String
    suspend fun summarize(noteContent: String): String
}

data class FlashcardResult(
    val question: String,
    val answer: String
)
