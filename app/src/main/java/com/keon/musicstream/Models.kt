package com.keon.musicstream

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val thumb: String,
)

sealed interface Load<out T> {
    data object Idle : Load<Nothing>
    data object Loading : Load<Nothing>
    data class Ok<T>(val data: T) : Load<T>
    data class Err(val message: String) : Load<Nothing>
}
