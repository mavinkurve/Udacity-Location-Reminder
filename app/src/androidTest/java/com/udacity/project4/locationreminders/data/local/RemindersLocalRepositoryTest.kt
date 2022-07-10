package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.utils.TestConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalToIgnoringCase
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {
    private lateinit var database: RemindersDatabase
    private lateinit var repository: RemindersLocalRepository

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initDB() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()

        repository = RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
    }

    @Test
    fun verifyRepositorySavesReminder() = runBlocking {
        // GIVEN
        val reminder = ReminderDTO(  TestConstants.TITLE,
            TestConstants.DESCRIPTION,
            TestConstants.LOCATION,
            TestConstants.LATITUDE,
            TestConstants.LONGITUDE)
        database.reminderDao().saveReminder(reminder)

        // WHEN
        val reminderDTO: Result<ReminderDTO> = repository.getReminder(reminder.id)
        val result: ReminderDTO = (reminderDTO as Result.Success).data

        // THEN
        assertThat(reminderDTO, CoreMatchers.notNullValue())
        assertThat(result.id, `is`(reminder.id))
    }

    @Test
    fun verifyEmptyRepositoryReturnsError() = runBlocking {
        repository.deleteAllReminders()
        val repoSaveReminder: Result<ReminderDTO> = repository.getReminder("1")
        val result: String? = (repoSaveReminder as Result.Error).message

        assertThat(repoSaveReminder, CoreMatchers.notNullValue())
        assertThat(result,equalToIgnoringCase("Reminder not found!"))
    }

    @After
    fun closeDB() {
        database.close()
    }
}
