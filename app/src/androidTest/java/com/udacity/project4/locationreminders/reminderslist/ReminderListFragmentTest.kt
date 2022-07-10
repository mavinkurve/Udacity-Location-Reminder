package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.assertion.ViewAssertions.matches

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.RemindersDao
import com.udacity.project4.locationreminders.data.local.RemindersDatabase
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.TestConstants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.mockito.Mockito.*


@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest :KoinTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // view model lazy inject
    private  val remindersListViewModel: RemindersListViewModel by inject()

    // reminders DAO lazy inject
    private val remindersDao : RemindersDao by inject()

    @Before fun startKoinForTest() {
        stopKoin()

        val module = module {
            viewModel {
                RemindersListViewModel(
                    getApplicationContext(),
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    getApplicationContext(),
                    get() as ReminderDataSource)
            }

            single {
                RemindersLocalRepository(get()) as ReminderDataSource
            }

            // in-memory database dao
            single {
                Room.inMemoryDatabaseBuilder(
                    getApplicationContext(),
                    RemindersDatabase::class.java
                )
                    // disable the main thread query check for Room
                    .allowMainThreadQueries()
                    .build()
                    .reminderDao()
            }
        }
        startKoin {
            androidLogger()
            androidContext(getApplicationContext())
            modules(listOf(module))
        }
    }


    @Test
    fun reminderListFragment_UITest() = runBlockingTest  {
        //GIVEN
        val reminder  = ReminderDTO(TestConstants.TITLE,
            TestConstants.DESCRIPTION,
            TestConstants.LOCATION,
            TestConstants.LATITUDE,
            TestConstants.LONGITUDE)
        remindersDao.saveReminder(reminder)

        //WHEN
        remindersListViewModel.loadReminders()
        launchFragmentInContainer<ReminderListFragment>(null,R.style.AppTheme)

        //THEN
        onView(withText(reminder.title)).check(matches(isDisplayed()))
        onView(withText(reminder.title)).check(matches(withText(TestConstants.TITLE)))
        onView(withText(reminder.description)).check(matches(isDisplayed()))
        onView(withText(reminder.description)).check(matches(withText(TestConstants.DESCRIPTION)))
        onView(withText(reminder.location)).check(matches(isDisplayed()))
        onView(withText(reminder.location)).check(matches(withText(TestConstants.LOCATION)))
    }



    @Test
    fun reminderListFragment_EmptyViewTest() = runBlockingTest {
        //GIVEN
        remindersDao.deleteAllReminders()

        //WHEN
        remindersListViewModel.loadReminders()
        launchFragmentInContainer<ReminderListFragment>(null,R.style.AppTheme)

        //THEN
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }

    @Test
    fun reminderListFragment_NavigationTest(){
        //GIVEN
        val mockNavController = mock(NavController::class.java)
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
            .onFragment { Navigation.setViewNavController(it.view!!,mockNavController) }

        //WHEN
        onView(withId(R.id.addReminderFAB)).perform(click())

        //THEN
        verify(mockNavController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }

    @After
    fun stopKoinAfterTest() = stopKoin()
}