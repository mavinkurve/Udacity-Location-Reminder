package com.udacity.project4.locationreminders.reminderslist

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.utils.TestConstants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class RemindersListViewModelTest {

    private val reminder1 = ReminderDTO(  TestConstants.TITLE,
        TestConstants.DESCRIPTION,
        TestConstants.LOCATION,
        TestConstants.LATITUDE,
        TestConstants.LONGITUDE)
    private val reminder2 = ReminderDTO(  TestConstants.TITLE,
        TestConstants.DESCRIPTION,
        TestConstants.LOCATION,
        TestConstants.LATITUDE,
        TestConstants.LONGITUDE)
    private val reminderList = mutableListOf<ReminderDTO>()
    private lateinit var remindersLocalDataSource: FakeDataSource

    @Before
    fun setupDataSource() = runBlockingTest {
        remindersLocalDataSource = FakeDataSource(reminderList)
    }

    @Test
    fun verifySuccessReturnedFromDataSource() = runBlockingTest {
        remindersLocalDataSource.saveReminder(reminder1)
        remindersLocalDataSource.saveReminder(reminder2)

        val reminders = remindersLocalDataSource.getReminders()

        assertThat((reminders as Result.Success).data.size, `is`(2))
    }

    @Test
    fun verifyErrorReturnedFromDataSource() = runBlockingTest {
        //GIVEN: Error in reminders
        remindersLocalDataSource.shouldReturnError()
        remindersLocalDataSource.saveReminder(reminder1)
        remindersLocalDataSource.saveReminder(reminder2)

        // WHEN - Get the reminders
        val reminders = remindersLocalDataSource.getReminders()

        // THEN - Error is returned
        assertThat((reminders as Result.Error),`is`(Result.Error("Not found")))

    }

    @Test
    fun verifyEmptyDataSource() = runBlockingTest {
        remindersLocalDataSource.deleteAllReminders()
        val reminders = remindersLocalDataSource.getReminders()
        assertThat((reminders as Result.Success).data.size, `is`(0))

    }
    @After
    fun cleanupDataSource() = runBlockingTest {
        stopKoin()
        remindersLocalDataSource.deleteAllReminders()
    }

}