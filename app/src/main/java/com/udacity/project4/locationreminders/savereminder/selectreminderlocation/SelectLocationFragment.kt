package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.annotation.VisibleForTesting
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragment
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*
import java.util.concurrent.TimeUnit

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var googleMap : GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient


    companion object{
        const  val REQUEST_CODE = 10
        const val ZOOM_LEVEL = 17f
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        //instantiate the SupportMapFragment and use the getMapAsync() method initialize the map
        val mapFragment = childFragmentManager.findFragmentById(R.id.fragment_map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return binding.root
    }

    /*
    Setting the selected Poi to viewModel and navigating back.
     */
    private fun onLocationSelected(selectedPoi : PointOfInterest) {
        Log.i("myTag","Selected Poi (Lat & Long ) : ${selectedPoi.name}  (${selectedPoi.latLng.latitude} , ${selectedPoi.latLng.longitude})")

        Snackbar.make(binding.root, "Save Location", Snackbar.LENGTH_INDEFINITE)
            .setAction("Save") {
                _viewModel.setPoi(selectedPoi)
                _viewModel.navigationCommand.postValue(
                    NavigationCommand.Back
                )
            }.show()

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE

            true
        }
        R.id.terrain_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        setMapStyle()

        if (checkForSelfPermission()) {
            val permission = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION ,Manifest.permission.ACCESS_COARSE_LOCATION)
            requestPermissions(permission, REQUEST_CODE)
        }
        else{
            //foreground permission is enabled now ask for location
            //Making sure
            if(deviceLocationEnabled()) {
                //location setting is enabled on user device
                enableMyLocation()
            }
            else
            {
                //ask for user to enable location service
                askUserToEnableLocationService()

            }
        }
        setMapLongClick(map)
        setPoiClickListener(map)
    }

    private fun askUserToEnableLocationService( resolve: Boolean = true){
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingClient = LocationServices.getSettingsClient(requireActivity())

        val locationSettingResponseTask = settingClient.checkLocationSettings(builder.build())

        locationSettingResponseTask.addOnFailureListener{
                exception ->
            if(exception is ResolvableApiException && resolve)
            {
                try {
                    startIntentSenderForResult(exception.resolution.intentSender,
                        REQUEST_CODE,null,0,0,0,null)

                }catch (sendEx : IntentSender.SendIntentException){
                    Log.e("myTag","Error : ${sendEx.message}")
                }
            }
            else
            {
                Snackbar.make(binding.root,
                    "TURN ON LOCATION SETTING", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Retry"){
                        askUserToEnableLocationService()
                    }.show()
            }
        }
        locationSettingResponseTask.addOnCompleteListener {
            Log.i("myTag","Settings Enabled")
            if(it.isSuccessful)
            {
                enableMyLocation()
            }
        }
    }

    //from https://stackoverflow.com/questions/20210565/android-location-manager-get-gps-location-if-no-gps-then-get-to-network-provid
    private fun deviceLocationEnabled(): Boolean {
        val locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun checkForSelfPermission(): Boolean = ActivityCompat.checkSelfPermission(requireContext(),
        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun enableMyLocation(){
        googleMap.isMyLocationEnabled = true
//using fused location provider client as it efficient in terms of battery usage.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        fusedLocationProviderClient.lastLocation.addOnSuccessListener {location->
            if (location!=null) {
                googleMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(location.latitude, location.longitude), ZOOM_LEVEL))
                Log.i("myTag","${location.latitude}, ${location.longitude}")
            }
        }

    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        if(REQUEST_CODE == requestCode)
        {
            if(grantResults.size>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED && grantResults[1] ==PackageManager.PERMISSION_GRANTED)
            {
                enableMyLocation()
            }else
            {
                Snackbar.make(binding.root,
                    getString(R.string.permission_denied_explanation),
                    Snackbar.LENGTH_INDEFINITE) // MAKING IT INDEFINITE TO LET USER ENABLE IF USER HAS OPTED FOR DON'T ASK AGAIN
                    .setAction("Enable") {
                        startActivity(Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        )
                    }.show()

            }
        }

    }

    private fun setMapLongClick(map: GoogleMap) {

        map.setOnMapLongClickListener { latLng ->
            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Random Location")
                    .snippet("${latLng.latitude},${latLng.longitude}")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
            Snackbar.make(binding.root, "Save Location", Snackbar.LENGTH_INDEFINITE)
                .setAction("Save") {
                    _viewModel.latitude.value = latLng.latitude
                    _viewModel.longitude.value = latLng.longitude
                    _viewModel.reminderSelectedLocationStr.value = "Random Location"
                    _viewModel.navigationCommand.postValue(
                        NavigationCommand.Back
                    )
                    //  fragmentManager?.popBackStack()
                }.show()

        }
    }





    private fun setPoiClickListener(gmap : GoogleMap) {

        gmap.setOnPoiClickListener {
            val poiMarker = gmap.addMarker(
                MarkerOptions().position(it.latLng)
                    .title(it.name)
                    .snippet("${it.latLng.latitude},${it.latLng.longitude}")
            )
            poiMarker?.showInfoWindow()
            onLocationSelected(it)
        }

    }

    private fun setMapStyle()
    {
        try {
            //val success = googleMap.setMapStyle()
           // if(!success)
         //   {
         //       Log.e("myMap","Style parsing Failed.")
         //   }
        }catch (e :Resources.NotFoundException)
        {
            Log.e("myMap","can't find the style. Error : ",e)
        }
    }




}
