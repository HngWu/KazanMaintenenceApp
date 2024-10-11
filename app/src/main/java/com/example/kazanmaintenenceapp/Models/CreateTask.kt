package com.example.kazanmaintenenceapp.Models

import kotlinx.serialization.Serializable

@Serializable
class CreateTask (
    val assetID: Int,
    val taskId: Int,
    val scheduleType: Int,
    val scheduleDate: String? = null,
    val scheduleKilometer: Int? = null,
    val taskDone: Boolean?,
    val odometerReading: Int? = null
)