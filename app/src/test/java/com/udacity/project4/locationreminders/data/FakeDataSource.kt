package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(var reminders: MutableList<ReminderDTO>? = mutableListOf()) : ReminderDataSource {

    private var shouldReturnError = false

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError) {
            return Result.Error("Not found")
        }
        reminders?.let { return Result.Success(ArrayList(it)) }
        return Result.Error("Not found")
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        val reminder = reminders?.find { it.id == id }

        return if(reminder!=null)
            Result.Success(reminder)
        else
            Result.Error("Not found")
    }

    override suspend fun deleteAllReminders() {
        reminders?.clear()
    }

    fun shouldReturnError() {
        shouldReturnError = true
    }
}