package com.udacity.project4.locationreminders.savereminder

import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeDataSource

import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem


import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.util.getOrAwaitValue
import com.udacity.project4.utils.TestConstants

import org.junit.Rule

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    private lateinit var viewModel: SaveReminderViewModel
    private val reminderList = mutableListOf<ReminderDTO>()
    private lateinit var remindersLocalDataSource: FakeDataSource

    // needed for getOrAwaitValue
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()


    @Before
    fun setupDataSourceViewModel() {
        runBlockingTest {
            stopKoin()
            remindersLocalDataSource = FakeDataSource(reminderList)
            viewModel = SaveReminderViewModel(getApplicationContext(), remindersLocalDataSource)
        }
    }

    @Test
    fun verifySavedReminderShowsToast(){
        val reminder = ReminderDataItem(  TestConstants.TITLE,
            TestConstants.DESCRIPTION,
            TestConstants.LOCATION,
            TestConstants.LATITUDE,
            TestConstants.LONGITUDE)
        viewModel.saveReminder(reminder)
        assertThat(viewModel.showSnackBarInt.getOrAwaitValue(),`is`(R.string.reminder_saved))
    }

    @After
    fun cleanupDataSource() = runBlockingTest {
        stopKoin()
        remindersLocalDataSource.deleteAllReminders()
    }
}

