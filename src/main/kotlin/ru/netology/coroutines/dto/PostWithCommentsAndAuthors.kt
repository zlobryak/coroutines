package ru.netology.coroutines.dto

data class PostWithCommentsAndAuthors(
    val post: Post,
    val comments: List<Comment>,
    val author: Author,
    val commentsAuthors: List<Author>

)
