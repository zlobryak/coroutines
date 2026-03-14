package ru.netology.coroutines

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import ru.netology.coroutines.dto.Author
import ru.netology.coroutines.dto.Comment
import ru.netology.coroutines.dto.Post
import ru.netology.coroutines.dto.PostWithCommentsAndAuthors
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private val gson = Gson()
private val BASE_URL = "http://127.0.0.1:9999"

// Клиент с логированием и таймаутами для всех сетевых запросов
private val client = OkHttpClient.Builder()
    .addInterceptor(HttpLoggingInterceptor(::println).apply {
        level = HttpLoggingInterceptor.Level.BODY
    })
    .connectTimeout(30, TimeUnit.SECONDS)
    .build()

fun main() {
    // Запускаем корутину в скоупе без диспетчера по умолчанию
    with(CoroutineScope(EmptyCoroutineContext)) {
        launch {
            try {
                // 1. Гребём посты
                // 2. На каждый пост вешаем async-задачу на подгрузку комментов
                // 3. awaitAll() ждёт, пока всё прилетит параллельно
                val posts = getPosts(client)
                    .map { post ->
                        async {
                            //Получим все комментарии
                            val comments = getComments(client, post.id)

                            val commentsAuthors = comments.map {
                                getAuthor(client, it.authorId)
                            }
                            // Получим всех авторов постов
                            val author = getAuthor(client, post.authorId)
                            PostWithCommentsAndAuthors(
                                post,
                                comments,
                                author,
                                commentsAuthors
                            )
                        }

                    }.awaitAll()
                // Выведем содержимое ответа в консоль
                println(posts)


            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    // Костыль: держим главный поток, чтобы корутины успели отработать
    Thread.sleep(30_000L)
}

// Обёртка над OkHttp Callback в suspend-функцию: превращает асинхронщину в линейный код
suspend fun OkHttpClient.apiCall(url: String): Response {
    return suspendCoroutine { continuation ->
        Request.Builder()
            .url(url)
            .build()
            .let(::newCall)
            .enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response)
                }

                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }
            })
    }
}

// Универсал: дёргает URL, проверяет статус, парсит JSON в нужный тип через Gson
suspend fun <T> makeRequest(url: String, client: OkHttpClient, typeToken: TypeToken<T>): T =
    withContext(Dispatchers.IO) {
        client.apiCall(url)
            .let { response ->
                if (!response.isSuccessful) {
                    response.close()
                    throw RuntimeException(response.message)
                }
                val body = response.body ?: throw RuntimeException("response body is null")
                gson.fromJson(body.string(), typeToken.type)
            }
    }

// GET /api/slow/posts — список постов
suspend fun getPosts(client: OkHttpClient): List<Post> =
    makeRequest("$BASE_URL/api/slow/posts", client, object : TypeToken<List<Post>>() {})

// GET /api/slow/posts/{id}/comments — комменты к конкретному посту
suspend fun getComments(client: OkHttpClient, id: Long): List<Comment> =
    makeRequest(
        "$BASE_URL/api/slow/posts/$id/comments",
        client,
        object : TypeToken<List<Comment>>() {})

// GET /api/slow/authors/{id} — авторы
suspend fun getAuthor(client: OkHttpClient, id: Long): Author =
    makeRequest("$BASE_URL/api/slow/authors/$id", client, object : TypeToken<Author>() {})