package com.sys4soft.deldia.models

data class Schedule(
    val scheduleId: Int = 0,
    val roleId: Int = 0,
    val roleName: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val daysOfWeek: String = "",
    val hasRestrictions: Boolean = false
)
