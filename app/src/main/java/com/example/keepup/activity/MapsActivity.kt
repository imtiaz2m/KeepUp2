package com.example.keepup.activity


import android.Manifest
import android.annotation.SuppressLint
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.example.keepup.service.MyJobService
import com.example.keepup.R
import com.example.keepup.model.Friends
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.firebase.database.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    var circle: Circle? = null
    var tempPoint: LatLng = LatLng(0.00, 0.00)
    var longT: Double = 0.00
    var latT: Double = 0.00
    var markersArrayList: ArrayList<Marker> = arrayListOf()

    var intentFriendId: String? = null

    val PERMISSION_ID = 42
    lateinit var mFusedLocationClient: FusedLocationProviderClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)


        /**
         *  This line points to the map fragment within the activity_maps xml
         *  and stores it as SupportMapFragment.
         */
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        var menuButton = findViewById(R.id.menuButton) as Button

        /**
         *  This method runs when the user clicks the Menu button.
         *  It calls openSettingsActivity that creates an intent to open the setting/friend list page.
         */
        menuButton.setOnClickListener {
            openSettingsActivity()
        }

        var button3 = findViewById(R.id.button3) as Button

        button3.setOnClickListener {
        }

        val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

        /**
         *  The following method runs a background jobservice.
         *  This helps in getting new user location and sending it to the databse.
         */
        val jobInfo =
            JobInfo.Builder(11, ComponentName(this@MapsActivity, MyJobService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).build()
        jobScheduler.schedule(jobInfo)
    }

    /**
     *  This method creates an intent that opens the MenuActivity i.e. the friends list page.
     */
    private fun openSettingsActivity() {
        val intent = Intent(this, MenuActivity::class.java)
        startActivity(intent)
    }



    /**
     *  This method checks if the users location is enabled.
     */
    private fun isLocationEnabled(): Boolean {
        var locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    /**
     *  This method checks if the required permissions are available.
     */
    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    /**
     *  This method gives the user a pop-up asking for location permissions.
     */
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            PERMISSION_ID
        )
    }

    /**
     *  This method runs getLastLocation method if the required permissions are granted.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_ID) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Granted. Start getting the location information
                getLastLocation()
            }
        }
    }

    /**
     *  This method is called in the requestNewLocationData method and updates the
     *  global variables of longT (longitude) and latT(latitude)
     */
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            var mLastLocation: Location = locationResult.lastLocation
            latT = mLastLocation.latitude
            longT = mLastLocation.longitude
        }
    }

    /**
     *  This method is called if no location is empty or null. It gets updated location of the user.
     */
    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        var mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 10000
        mLocationRequest.fastestInterval = 5000
        mLocationRequest.numUpdates = 1

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient!!.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }


    /**
     *  This method calls other mthods to check if the required persmission are there and if the user has turned on the location toggle.
     *  It then gets the users location.
     *  This method is called every time this activity is loaded.
     */
    @SuppressLint("MissingPermission")
    private fun getLastLocation() {

        if (checkPermissions()) {
            if (isLocationEnabled()) {

                mFusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                    var location: Location? = task.result
                    if (location == null) {
                        requestNewLocationData()
                    } else {
                        latT = location.latitude
                        longT = location.longitude
                        println("lont: " + longT.toString())
                        println("latT:" + latT.toString())
                        val myPos = LatLng(latT, longT)
                        mMap.addMarker(MarkerOptions().position(myPos).title("My Marker"))
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(myPos))
                    }
                }
            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }

    /**
     *  This method is passed the longitude and latitude of a friends location along wither email address.
     *  It then prints a marker on friends' location.
     *  It is called when an intent is passed from the menu activity which holds a string of all friends' info.
     *  It runs with the onMapReady method and is called either once or in a loop depending on the number of friends passed by intent.
     */
    @SuppressLint("MissingPermission")
    private fun getFriendLastLocation(
        longT: String,
        latT: String,
        EmailAdd: String,
        multipleFriends: Boolean
    ) {
        var tempL1: Double = latT.toDouble()
        var tempL2: Double = longT.toDouble()

        val FriendLocation = LatLng(tempL1, tempL2)
        if (mMap != null) {
            Log.d("MapNull", "Not Null***********")
            if (multipleFriends) {
                markersArrayList.add(mMap.addMarker(MarkerOptions().position(FriendLocation).title("Marker of " + EmailAdd)))
                Log.d("MultipleFriends", EmailAdd + "***********")
            } else {
                mMap.addMarker(MarkerOptions().position(FriendLocation).title("Marker of " + EmailAdd))
                mMap.moveCamera(CameraUpdateFactory.newLatLng(FriendLocation))
            }
        } else Log
            .d("MapIsNull", "Map is null************")

    }

    /**
     *  All markers created for friends are stored within a globale list called markersArrayList.
     *  This method  tries to focus the camera on the map into a central position
     *  with respect to all markers so all markers are visible.
     */
    private fun getAllMarkersCamera(markers: ArrayList<Marker>): CameraUpdate {
        var builder: LatLngBounds.Builder =
            LatLngBounds.builder()// = new google.maps.LatLngBounds();
        for (i in 0 until markers.size) {
            builder.include(markers[i].position)//extend(markers[i].getPosition());
        }
        val bounds = builder.build()

        var padding: Int = 0 // offset from edges of the map in pixels
        var camUpdate: CameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)

        return camUpdate
    }

    /**
     *  This method is executed when the map framgemnt is loaded.
     *  Since it is the only method that provides the GoogleMap object, this method
     *  includes code to call methods such as getFriendsLocation to update
     *  positions on the map.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val intent = intent
        val intentEmail = intent.getStringExtra("emailAddress")
        val intentLongT = intent.getStringExtra("longT")
        val intentLatT = intent.getStringExtra("latT")
        intentFriendId = intent.getStringExtra("friendId")
        val intentgeoFenceLatT = intent.getStringExtra("geoFenceLatT")
        val intentgeoFenceLongT = intent.getStringExtra("geoFenceLongT")


        /**
         *  We pass the friend data using the intent that is passed from the menu activity.
         *  It checks if the intent has any data regarding the friend, if yes,
         *  it executes the getFriendLastLocation method.
         */
        if (intentEmail == "" || intentEmail == null) {
            getLastLocation()
            Log.d("IntentIF", "In**********")
        } else {
            Log.d("IntentElse", intentEmail.toString())
            if (intentEmail.contains('~')) {
                val tempEmailList: List<String> = intentEmail.split('~')
                var tempLatTList: List<String> = intentLatT.split('~')
                var tempLongtList: List<String> = intentLongT.split('~')


                Log.d("tempEmailList", intentEmail)
                for (i in 0 until tempEmailList.count()) {
                    getFriendLastLocation(tempLongtList[i], tempLatTList[i], tempEmailList[i], true)
                }

                mMap.animateCamera(getAllMarkersCamera(markersArrayList))
                markersArrayList.clear()
            } else {
                getFriendLastLocation(intentLongT, intentLatT, intentEmail, false)
                Log.d("intentgeoFenceLatt", intentgeoFenceLatT)

                tempPoint = LatLng(intentgeoFenceLatT.toDouble(), intentgeoFenceLongT.toDouble())

                if(tempPoint.latitude != 0.0){
                    var circleOptions: CircleOptions = CircleOptions()
                        .center(LatLng(tempPoint.latitude, tempPoint.longitude))
                        .radius(1000.00)
                        .fillColor(0x40ff0000)
                        .strokeColor(Color.TRANSPARENT)
                        .strokeWidth(2.0F)

                    circle = mMap.addCircle(circleOptions)
                    mMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(
                                tempPoint.latitude,
                                tempPoint.longitude
                            ), 12.5f
                        )
                    )
                }else{
                    Toast.makeText(applicationContext, "No Circle set on this friend", Toast.LENGTH_LONG).show()
                }

            }


        }

        /**
         *  This method is executed when the user touches the screen of the map.
         *  It creates new geo-fences at the position the user touches for
         *  friends that are loaded at a current time.
         */
        mMap.setOnMapClickListener(object : GoogleMap.OnMapClickListener {
            override fun onMapClick(point: LatLng) {

                //Toast.makeText(applicationContext, point.latitude.toString() + ", " + point.longitude.toString(), Toast.LENGTH_LONG).show()
                drawCircle(point)
                mMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            point.latitude,
                            point.longitude
                        ), 12.5f
                    )
                )
                updateFriendsGeoFence()
            }
        })


    }

    /**
     *  This method creates the actual geo-fence object on the screen.
     *  It does not create a goolge geo-fence object but instead uses the
     *  circle object that is displayed on the map.
     */
    private fun drawCircle(point: LatLng) {
        tempPoint = point

        var circleOptions: CircleOptions = CircleOptions()
            .center(LatLng(point.latitude, point.longitude))
            .radius(1000.00)
            .fillColor(0x40ff0000)
            .strokeColor(Color.TRANSPARENT)
            .strokeWidth(2.0F)


        if (circle != null) {
            circle?.remove()
        }
        circle = mMap.addCircle(circleOptions)
    }


    /**
     *  This method updates the geo-fence saved for a friend. If the user
     *  clicks anywhere on the screen it takes those points og longitude and latitude and updates the information
     *  for a friends geo-fence saved in the database.
     */
    private fun updateFriendsGeoFence() {
        val settings = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        var currentID: String? = settings.getString("id", null)
        var FriendDataBase: DatabaseReference =
            FirebaseDatabase.getInstance().getReference("Friends")
        Log.d("currentIDCheck", currentID)
        FriendDataBase.orderByChild("id")
            .equalTo(currentID)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.value != null) {
                        Log.d("SnapShotPrint", dataSnapshot.toString())
                        dataSnapshot.children.forEach {
                            var FriendId: String = it.child("friendId").getValue().toString()
                            Log.d("equalFriendID", FriendId + " = " + intentFriendId)
                            if (FriendId == intentFriendId) {
                                Log.d("matchFriendID", "True")
                                var snapFriendId: String = it.key.toString()
                                Log.d("snapFirendId", snapFriendId)
//                                    var SnapFriendId = FriendDataBase?.push()?.key!!
                                var NewFriend: Friends =
                                    Friends(
                                        currentID.toString(),
                                        FriendId,
                                        tempPoint.latitude.toString(),
                                        tempPoint.longitude.toString()
                                    )
                                FriendDataBase?.child(snapFriendId.toString())?.setValue(NewFriend)

                            }

                        }
                        Log.d("withinIf", "**********************************************")
                    } else {
                        //Toast.makeText(this@MenuActivity, "User does not exist", Toast.LENGTH_LONG)
                    }//ToDo: Need to fix
                }

                override fun onCancelled(databaseError: DatabaseError) {
//                    // handle error
//
                }
            })


    }

}
