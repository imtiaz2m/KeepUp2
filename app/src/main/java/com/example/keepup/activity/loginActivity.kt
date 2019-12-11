package com.example.keepup.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import java.time.LocalDateTime
import android.annotation.SuppressLint
import android.location.Location
import android.util.Log
import androidx.preference.PreferenceManager
import com.example.keepup.R
import com.example.keepup.model.User
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.firebase.database.*


class loginActivity : AppCompatActivity() {

    var addUserEmail: EditText? = null
    var addUserName: EditText? = null
    lateinit var database: DatabaseReference
    var longitude: String = ""
    var latitude: String = ""
    internal var user: User? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        /**
         *   This line of code gets the "User" node from the firebase database.
         */
        database = FirebaseDatabase.getInstance().getReference("User")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        ObtainLocation()

        addUserEmail = findViewById(R.id.enterUserEmail) as EditText
        addUserName = findViewById(R.id.enterUserName) as EditText
        var addUserButton = findViewById(R.id.addUser) as Button

        addUserButton.setOnClickListener() {
            var check : Boolean = false
           check = addUser()

            if(check)
            openMapsActivity()
        }
    }

    /**
     *  This method runs when the activity is created. It gets the users current location.
     */
    @SuppressLint("MissingPermission")
    private fun ObtainLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                latitude = location?.latitude.toString()
                longitude = location?.longitude.toString()
            }
    }


    /**
    * This method is called when the user clicks on the login button.
    * It either creates a new user in case of email address not found ot gets users data and creates a session.
     **/
    private fun addUser():Boolean {
        var userEmail = addUserEmail?.text.toString().trim()
        var userName = addUserName?.text.toString().trim()
        var id: String? = null

        /**
         *  The following code is used throughtout the application to query data
         *  and get or set values in the database.
         *  In the following case we are looking for the users email address
         *  and checking if it exists in the databse or not.
         *  If yes, we get the specific User object from the database.
         *  And save it to our session parameters.
         */
        if (!TextUtils.isEmpty(userEmail) && !TextUtils.isEmpty(userName)) {
            database.orderByChild("emailAddress")
                .equalTo(userEmail)
                .limitToFirst(1)
                .addListenerForSingleValueEvent(object : ValueEventListener {

                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        Log.d("dataSnapshot", dataSnapshot.toString())
                        if (dataSnapshot.value != null) {

                            dataSnapshot.children.forEach {
                                id = it.child("id").getValue().toString()
                                Log.d("GotID", "True**************************")
                                setGlobalSession(userEmail, id, userName)
                            }
                        }
                        else {
                            Log.d("Found", "NOTHING******************************************************************")
                            id = database?.push()?.key!!
                            var user: User = User(userName, userEmail, id.toString(), longitude, latitude, LocalDateTime.now())
                            database?.child(id.toString())?.setValue(user)
                            setGlobalSession(userEmail, id, userName)
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {

                    }
                })
            return true
        }
        else {
            Toast.makeText(this, "Please fill both User name and Email Address fields.", Toast.LENGTH_LONG).show()
            return false
        }
    }


    /**
     *  This method is used to set the session variables stored. The session object
     *  has the user data stores to be accessed at any time while the app is running.
     */
    private fun setGlobalSession(userEmail: String, id: String?, userName: String) {
        val settings = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val editor = settings.edit()
        editor.putString("userEmail", userEmail)
        editor.putString("id", id)
        editor.putString("userName", userName)
        editor.commit()
    }


    /**
     *  This method creates an intent to open the MapsActivity.
     *  It is called after the user clicks login and addUser method runs successfully.
     */
    private fun openMapsActivity() {
        val intent = Intent(this, MapsActivity::class.java)
        startActivity(intent)
    }


}
