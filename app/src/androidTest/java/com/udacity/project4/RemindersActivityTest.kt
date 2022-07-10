package com.udacity.project4

import android.app.Activity
import android.app.Application
import android.os.Build
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.EspressoIdlingResource
import com.udacity.project4.utils.TestConstants
import com.udacity.project4.util.monitorActivity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {
    // Extended Koin Test - embed autoclose @after method to close Koin after every test

    private fun getActivity(activityScenario: ActivityScenario<RemindersActivity>): Activity? {
        var activity: Activity? = null
        activityScenario.onActivity {
            activity = it
        }
        return activity
    }
    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application



    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        Log.i("myTag","Version :${BuildConfig.VERSION_CODE} Sdk : ${Build.VERSION.SDK_INT}" )
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()
        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    private val dataBindingIdlingResource : DataBindingIdlingResource = DataBindingIdlingResource()

    @Before
    fun registerIdlingResource(){
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @Test
    fun verifyAddReminder() {
        // Launch Reminders Activity
        val remindersActivity = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(remindersActivity)

        // Add reminder
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(typeText(TestConstants.TITLE))
        onView(withId(R.id.reminderDescription)).perform(typeText(TestConstants.DESCRIPTION))
        onView(withId(R.id.selectLocation)).perform(click())
        onView(withContentDescription("Google Map")).perform(longClick())
        // the IdlingResource registry has been intermittely failing, adding explicit wait to improve
        // test reliability
        Thread.sleep(1000)
        onView(withText("SAVE")).perform(click())
        onView(withId(R.id.saveReminder)).perform(click())

        // Verify added reminder is shown
        onView(withText(TestConstants.TITLE)).check(matches(isDisplayed()))

        // close activity
        remindersActivity.close()
    }

    @Test
    fun verifySavedReminderShowsUp() = runBlocking {
        //Add reminder to repository
        val reminder = ReminderDTO(
            TestConstants.TITLE,
            TestConstants.DESCRIPTION,
            TestConstants.LOCATION,
            TestConstants.LATITUDE,
            TestConstants.LONGITUDE)
        repository.saveReminder(reminder)

        // start activity
        val reminderActivity = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(reminderActivity)

        // verify activity shows up
        onView(withId(R.id.title)).check(matches(isDisplayed()))
        onView(withId(R.id.title)).check(matches(withText(TestConstants.TITLE)))

        reminderActivity.close()
    }


    @Test
    fun verifyErrorPrompt() {

        val reminderActivity = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(reminderActivity)

        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(typeText(TestConstants.TITLE))
        onView(withId(R.id.reminderDescription)).perform(typeText(TestConstants.DESCRIPTION))
        onView(withId(R.id.saveReminder)).perform(click())

        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(isDisplayed()))
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(withText(R.string.error_prompt_no_location)))
    }

    @After
    fun unRegisterIdlingResource(){
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }
}
