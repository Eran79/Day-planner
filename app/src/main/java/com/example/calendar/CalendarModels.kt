package com.example.calendar

data class DeviceCalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val accountType: String,
    val color: Int,
    val isPrimary: Boolean
)

data class SyncResult(
    val success: Boolean,
    val syncedCount: Int,
    val message: String
)
