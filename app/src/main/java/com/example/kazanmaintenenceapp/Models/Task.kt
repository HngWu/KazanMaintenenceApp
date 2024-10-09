package com.example.kazanmaintenenceapp.Models


data class Task(
    val assetName: String,
    val assetSN: String,
    val taskName: String,
    val scheduleType: String,
    val scheduleDate: String? = null,
    val scheduleKilometer: Int? = null,
    var taskDone: Boolean
)