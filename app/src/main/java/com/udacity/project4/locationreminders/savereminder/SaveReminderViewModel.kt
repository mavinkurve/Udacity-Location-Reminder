package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.text.BoringLayout
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.PointOfInterest
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.authentication.FirebaseUserLiveData
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.dsl.ATTRIBUTE_VIEW_MODEL

class SaveReminderViewModel(val app: Application, val dataSource: ReminderDataSource) :
    BaseViewModel(app) {

    private var geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(app.applicationContext)

    private val goeFencingPendingIntent by lazy{
        val intent = Intent(app, GeofenceBroadcastReceiver::class.java)
        intent.action = SaveReminderFragment.ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(app.applicationContext ,0,intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }


    val reminderTitle = MutableLiveData<String?>()
    val reminderDescription = MutableLiveData<String?>()
    val reminderSelectedLocationStr = MutableLiveData<String?>()
    private var _selectedPOI = MutableLiveData<PointOfInterest>()
    val selectedPoi : LiveData<PointOfInterest>
        get() = _selectedPOI

    val latitude = MutableLiveData<Double>()
    val longitude = MutableLiveData<Double>()


    /*
    set the poi, title of poi to textview , latitude and longitude
     */
    fun setPoi(poi :PointOfInterest)
    {
        _selectedPOI.value = poi
        latitude.value = poi.latLng.latitude
        longitude.value = poi.latLng.longitude
        reminderSelectedLocationStr.value = poi.name

    }


    /**
     * Clear the live data objects to start fresh next time the view model gets called
     */

    fun onClear() {
        reminderTitle.value = null
        reminderDescription.value = null
        reminderSelectedLocationStr.value = null
        _selectedPOI.value = null
        latitude.value = null
        longitude.value = null
    }

    /**
     * Validate the entered data then saves the reminder data to the DataSource
     */
    fun validateAndSaveReminder(reminderData: ReminderDataItem) {
        if (validateEnteredData(reminderData)) {
            saveReminder(reminderData)
        }

    }




    /**
     * Save the reminder to the data source
     */
    fun saveReminder(reminderData: ReminderDataItem) {
        showLoading.value = true
        viewModelScope.launch {
            dataSource.saveReminder(
                ReminderDTO(
                    reminderData.title,
                    reminderData.description,
                    reminderData.location,
                    reminderData.latitude,
                    reminderData.longitude,
                    reminderData.id
                )
            )
            showLoading.value = false
            showToast.value = app.getString(R.string.reminder_saved)
            navigationCommand.value = NavigationCommand.Back
        }
    }

    /**
     * Validate the entered data and show error to the user if there's any invalid data
     */
    fun validateEnteredData(reminderData: ReminderDataItem): Boolean {
        if (reminderData.title.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_enter_title
            return false
        }

        if (reminderData.location.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_select_location
            return false
        }
        return true
    }

    /*
        Add geofencing and once activated show user about it.
     */
    @SuppressLint("MissingPermission")
    fun addGeofencing(reminderData: ReminderDataItem){
        //Use the dwell transition type to reduce alert spam
        val geofence = Geofence.Builder().apply {
            setRequestId(reminderData.id)
            setCircularRegion(latitude.value!!,longitude.value!!,100f)
            setExpirationDuration(Geofence.NEVER_EXPIRE)
            setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            setLoiteringDelay(120000) //2 minutes.
        }.build()

        val geofencingRequest = GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofence(geofence)
        }.build()


        geofencingClient.addGeofences(geofencingRequest,goeFencingPendingIntent)?.run {
            addOnSuccessListener {
                Log.i("myTag","Success!! Geofencing Activated")
                showSnackBar.value = app.applicationContext.getString(R.string.successful_GeoFence_Activation_message)
                //we are sure here that all location permission is enabled
                validateAndSaveReminder(reminderData)

            }
            addOnFailureListener{
                Log.i("myTag","Failed!! Geofencing Failed")
                showSnackBar.value =app.applicationContext.getString(R.string.failed_Geofence_Activation_Message)

            }
        }

    }



}


