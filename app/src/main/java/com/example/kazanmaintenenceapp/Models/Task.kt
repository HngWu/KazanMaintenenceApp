package com.example.kazanmaintenenceapp.Models

import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val id: Int,
    val assetName: String,
    val assetSN: String,
    val taskName: String,
    val scheduleType: String,
    val scheduleDate: String? = null,
    val scheduleKilometer: Int? = null,
    var taskDone: Boolean
)