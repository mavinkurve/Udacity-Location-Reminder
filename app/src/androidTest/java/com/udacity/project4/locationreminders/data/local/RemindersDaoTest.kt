package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    @get: Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()
    private lateinit var database: RemindersDatabase

    @Before
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java).allowMainThreadQueries().build()
    }

    @Test
    fun verifyCanSaveReminder() = runBlockingTest {
        // GIVEN
        val reminder = ReminderDTO("Test Reminder Title",
            "Test Reminder Description",
            "Test Reminder Location",
            1.0,
            1.0)
        database.reminderDao().saveReminder(reminder)

        // WHEN
        val reminderDTO = database.reminderDao().getReminderById(reminder.id)

        // THEN
        assertThat<ReminderDTO>(reminderDTO as ReminderDTO, notNullValue())
        assertThat(reminderDTO.id, `is`(reminder.id))
        assertThat(reminderDTO.title, `is`(reminder.title))
        assertThat(reminderDTO.description, `is`(reminder.description))
        assertThat(reminderDTO.location, `is`(reminder.location))
        assertThat(reminderDTO.latitude, `is`(reminder.latitude))
        assertThat(reminderDTO.longitude, `is`(reminder.longitude))
    }

    @After
    fun closeDb() = runBlockingTest {
        database.reminderDao().deleteAllReminders()
        database.close()
    }
}