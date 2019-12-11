package com.example.keepup.service;

import android.annotation.SuppressLint
import android.app.job.JobParameters
import android.app.job.JobService
import android.location.Location
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.example.keepup.model.User
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.time.LocalDateTime

class MyJobService : JobService() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    var longitude: String? = null
    var latitude: String? = null
    var database: DatabaseReference? = null

    override fun onStopJob(params: JobParameters?): Boolean {
        return true//  TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onStartJob(parameters: JobParameters?): Boolean {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        ObtainLocation()
        return true
    }


    @SuppressLint("MissingPermission")
    private fun ObtainLocation() {
        fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    latitude = location?.latitude.toString()
                    longitude = location?.longitude.toString()

                    Toast.makeText(this, "ObtainLocationJobService" + latitude.toString(), Toast.LENGTH_SHORT).show()
                    updateUser()

                }
    }

    private fun updateUser() {

        database = FirebaseDatabase.getInstance().getReference("User")
        val settings = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        var currentEmail: String? = settings.getString("userEmail", null)
        var currentID: String? = settings.getString("id", null)
        var currentUserName: String? = settings.getString("userName", null)

        var user: User = User(
                currentUserName.toString(),
                currentEmail.toString(),
                currentID.toString(),
                latitude.toString(),
                longitude.toString(),
                LocalDateTime.now()
        )

        database?.child(currentID.toString())?.setValue(user)
        Toast.makeText(this, "User added in job service", Toast.LENGTH_LONG).show()
    }

}


