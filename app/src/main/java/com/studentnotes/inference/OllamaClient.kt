package com.studentnotes.inference

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class OllamaClient(
    private val baseUrl: String = "http://10.0.2.2:11434",
    private val model: String = "qwen2.5:0.5b"
) : InferenceClient {

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 120_000
        }
    }

    override suspend fun generateFlashcards(noteContent: String): List<FlashcardResult> {
        val prompt = """
            Based on the following study notes, generate 5 flashcards in JSON format.
            Each flashcard should have a "question" and an "answer" field.
            Return only valid JSON array, no other text.

            Notes:
            $noteContent

            Response format:
            [{"question": "...", "answer": "..."}, ...]
        """.trimIndent()

        val response = generate(prompt)
        return parseFlashcards(response)
    }

    override suspend fun answerQuestion(noteContent: String, question: String): String {
        val prompt = """
            Based on the following study notes, answer the question.
            Be concise and accurate. If the answer is not in the notes, say so.

            Notes:
            $noteContent

            Question: $question
        """.trimIndent()

        return generate(prompt)
    }

    override suspend fun summarize(noteContent: String): String {
        val prompt = """
            Summarize the following study notes in a clear, concise manner.
            Focus on key concepts and main points.

            Notes:
            $noteContent
        """.trimIndent()

        return generate(prompt)
    }

    private suspend fun generate(prompt: String): String {
        val request = OllamaRequest(
            model = model,
            prompt = prompt,
            stream = false
        )

        val response: OllamaResponse = client.post("$baseUrl/api/generate") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

        return response.response
    }

    private fun parseFlashcards(response: String): List<FlashcardResult> {
        return try {
            val jsonStart = response.indexOf('[')
            val jsonEnd = response.lastIndexOf(']') + 1
            if (jsonStart == -1 || jsonEnd == 0) {
                emptyList()
            } else {
                val jsonString = response.substring(jsonStart, jsonEnd)
                Json.decodeFromString<List<FlashcardResponse>>(jsonString)
                    .map { FlashcardResult(question = it.question, answer = it.answer) }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun close() {
        client.close()
    }
}

@Serializable
private data class OllamaRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean
)

@Serializable
private data class OllamaResponse(
    @SerialName("response")
    val response: String
)

@Serializable
private data class FlashcardResponse(
    val question: String,
    val answer: String
)
