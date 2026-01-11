package com.studentnotes.inference

class FakeInferenceClient : InferenceClient {

    var summarizeResponse: String = "This is a fake summary."
    var flashcardsResponse: List<FlashcardResult> = listOf(
        FlashcardResult("What is Kotlin?", "A modern programming language for the JVM."),
        FlashcardResult("What is Compose?", "A declarative UI toolkit for Android.")
    )
    var answerResponse: String = "This is a fake answer."

    var shouldThrowError: Boolean = false
    var errorMessage: String = "Fake inference error"

    var summarizeCallCount: Int = 0
        private set
    var lastSummarizeInput: String? = null
        private set

    override suspend fun generateFlashcards(noteContent: String): List<FlashcardResult> {
        if (shouldThrowError) {
            throw RuntimeException(errorMessage)
        }
        return flashcardsResponse
    }

    override suspend fun answerQuestion(noteContent: String, question: String): String {
        if (shouldThrowError) {
            throw RuntimeException(errorMessage)
        }
        return answerResponse
    }

    override suspend fun summarize(noteContent: String): String {
        summarizeCallCount++
        lastSummarizeInput = noteContent
        if (shouldThrowError) {
            throw RuntimeException(errorMessage)
        }
        return summarizeResponse
    }

    fun reset() {
        summarizeResponse = "This is a fake summary."
        flashcardsResponse = listOf(
            FlashcardResult("What is Kotlin?", "A modern programming language for the JVM."),
            FlashcardResult("What is Compose?", "A declarative UI toolkit for Android.")
        )
        answerResponse = "This is a fake answer."
        shouldThrowError = false
        errorMessage = "Fake inference error"
        summarizeCallCount = 0
        lastSummarizeInput = null
    }
}
