package com.codetoday.remembrance.data.repository

import com.codetoday.remembrance.data.db.ReminderDao
import com.codetoday.remembrance.data.model.ReminderItem
import kotlinx.coroutines.flow.Flow

class ReminderRepository(private val reminderDao: ReminderDao) {

    val allReminders: Flow<List<ReminderItem>> = reminderDao.getAllReminders()

    suspend fun getActiveReminders(): List<ReminderItem> = reminderDao.getActiveReminders()

    suspend fun insert(reminder: ReminderItem): Long = reminderDao.insertReminder(reminder)

    suspend fun update(reminder: ReminderItem) = reminderDao.updateReminder(reminder)

    suspend fun delete(reminder: ReminderItem) = reminderDao.deleteReminder(reminder)

    suspend fun deleteById(id: Long) = reminderDao.deleteReminderById(id)

    suspend fun getCount(): Int = reminderDao.getCount()
}
