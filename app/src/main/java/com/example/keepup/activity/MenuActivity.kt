package com.example.keepup.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.*
import androidx.preference.PreferenceManager
import com.example.keepup.MyCallback
import com.example.keepup.R
import com.example.keepup.model.Friends
import com.example.keepup.model.User
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.*
import java.time.LocalDateTime

class MenuActivity : AppCompatActivity() {

    var addUserEmail: EditText? = null
    var userList = mutableListOf<User>()
    var pointList = mutableListOf<LatLng>()
    var listItems = mutableListOf<String>()

    lateinit var database: DatabaseReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        var homeButton = findViewById(R.id.homeButton) as Button

        /**
         *  Accessing the user node in the database here.
         */
        database = FirebaseDatabase.getInstance().getReference("User")


        /**
         *  This event is used to execute an intent within openMapsActivity
         *  that opens the mapActivity and also passes some friend data
         *  on to the mapsActivity depending on the users actions.
         */
        homeButton.setOnClickListener {
            openMapsActivity()
        }

        addUserEmail = findViewById(R.id.enterUserEmail) as EditText
        var addUserButton = findViewById(R.id.addUser) as Button
        searchFriends()


        /**
         *  This event is run when the user adds a new friend.
         *  It calls the addUser method.
         */
        addUserButton.setOnClickListener()
        {
            addFriends()
            Log.d("CrossPoint3", "CrossPoint3*******************")
        }

        var seeAllFriendsBtn = findViewById(R.id.seeAllFriends) as Button

        /**
         *  This method is executed when the user clicks on View All Friends On Map button.
         */
        seeAllFriendsBtn.setOnClickListener()
        {
            getAllFriendsLocation()
        }

        var friendsList: ListView = findViewById<ListView>(R.id.friendsList)
        friendsList.setOnItemClickListener { parent, view, position, id ->

            /**
             *  Thw following code is taking friend data stored in a list and passing it within an
             *  intent variable to the maps activity.
             */
            val intent = Intent(this, MapsActivity::class.java)
            intent.putExtra("emailAddress", userList[position].emailAddress)
            intent.putExtra("longT", userList[position].longT)
            intent.putExtra("latT", userList[position].latT)
            intent.putExtra("friendId", userList[position].id)
            intent.putExtra("geoFenceLatT", pointList[position].latitude.toString())
            intent.putExtra("geoFenceLongT", pointList[position].longitude.toString())

            Log.d("puttingInContent", pointList[position].latitude.toString())
            startActivity(intent)

            Log.d("PositionInList", position.toString())
        }


    }

    /**
     *  addFriends here is used to add friends to a users list.
     */
    private fun addFriends() {
        var userEmail = addUserEmail?.text.toString().trim()
        var FriendDataBase: DatabaseReference =
            FirebaseDatabase.getInstance().getReference("Friends")

        if (!TextUtils.isEmpty(userEmail)) {
            database.orderByChild("emailAddress")
                .equalTo(userEmail)
                .limitToFirst(1)
                .addListenerForSingleValueEvent(object : ValueEventListener {

                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.value != null) {
                            Log.d("SnapShotPrint", dataSnapshot.toString())
                            dataSnapshot.children.forEach {
                                val settings = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                                var currentID: String? = settings.getString("id", null)

                                var FriendId: String = it.child("id").getValue().toString()

                                var SnapFriendId = FriendDataBase?.push()?.key!!
                                var NewFriend: Friends = Friends(currentID.toString(), FriendId, "", "")
                                FriendDataBase?.child(SnapFriendId.toString())?.setValue(NewFriend)
                                searchFriends()
                                Log.d("withinFor", "**********************************************")
                            }
                            Log.d("withinIf", "**********************************************")
                        } else {
                            Toast.makeText(this@MenuActivity, "User does not exist", Toast.LENGTH_LONG)
                        }//ToDo: Need to fix
                    }

                    override fun onCancelled(databaseError: DatabaseError) {

                    }
                })
        }
    }


    /**
     *  This method starts an intent to open the maps activity.
     */
    private fun openMapsActivity() {
        val intent = Intent(this, MapsActivity::class.java)
        startActivity(intent)
    }



    /**
     *  This method is executed first when the menu activity is loaded to
     *  update the list of all friends for the user. It searches for all friends
     *  within the databse for a user using the user id as key in the friends node
     *  of the databse.
     */
    private fun searchFriends() {
        val settings = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        var currentID: String? = settings.getString("id", null)
        var FriendDataBase: DatabaseReference = FirebaseDatabase.getInstance().getReference("Friends")

        FriendDataBase.orderByChild("id")
            .equalTo(currentID)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    if (dataSnapshot.value != null) {
                        Log.d("DataSnapshot", "Not null************")

                        dataSnapshot.children.forEach {

                            Log.d("AttemptToString", "************")
                            var FriendId: String = it.child("friendId").getValue().toString()
                            var geofenceLatT: String = it.child("latT").getValue().toString()
                            var geoFenceLongT: String = it.child("longT").getValue().toString()

                            getUser(FriendId, geofenceLatT, geoFenceLongT, object : MyCallback {

                                override fun onCallback(e: User, tempGeoLatT: String, tempGeoLongT: String) {
                                    Log.d("UserAdd", e.emailAddress + "  Sucesss************")

                                    fillFriendList(e,tempGeoLatT,tempGeoLongT)
                                    if (tempGeoLatT != "") {
                                        Log.d("tempGeoLatT", tempGeoLatT)
                                        pointList.add(LatLng(tempGeoLatT.toDouble(), tempGeoLongT.toDouble()))
                                    } else {
                                        Log.d("ElsetempGeoLatT", tempGeoLatT)
                                        pointList.add(LatLng(0.00, 0.00))
                                    }
                                }
                            })
                            Log.d("FriendId", FriendId)
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
//                    // handle error
                }
            })

        Log.d("searchFrOut", "in************")

    }

    /**
     *  This method is called after searchFriends to add the friends into
     *  the list view under the add Friends button.
     */
    private fun fillFriendList(user: User,latCircle: String, longCircle:String) {
        val result = isPointInCircle(latCircle.toDouble(),longCircle.toDouble(),user.latT.toDouble(),user.longT.toDouble());
        var resultStatus=" ";
        if(result == true && latCircle!="")
                resultStatus="In Bounds"
        else
             resultStatus ="Not In Bounds"

        Log.d("fillFrIn", "in************")
        lateinit var listView: ListView
        listView = findViewById<ListView>(R.id.friendsList)

        var check: Boolean = false
        for (i in 0 until userList.count()) {
            if (userList[i].emailAddress == user.emailAddress) {
                check = true
            }
        }
        if (check == false) {
            userList.add(user)
            listItems.add(user.name + " (" + user.emailAddress + ")"+resultStatus)
        }

        Log.d("CrossPoint", "CrossPoint*******************")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listItems)
        listView.adapter = adapter
        Log.d("CrossPoint2", "CrossPoint2*******************")
    }


    /**
     *  This method checks if a certain location is within a certain area.
     *  It uses a formula which is passed the longitude and latitude of
     *  a person and the longitude and latitude of that person ge-fence.
     *  It then calculates if the user is within the bounds or not and returns
     *  a boolean value.
     */
    private fun isPointInCircle(latCircle: Double, longCircle:Double, friendlat:Double, friendlong:Double): Boolean{
        Log.d("CrossPoint99", "CrossPoint99*******************")
        Log.d("CrossPoint99", latCircle.toString()+" "+longCircle.toString()+" "+friendlat.toString()+" "+friendlong.toString())

        if(latCircle == 0.0)
            return true

        if ((friendlat - latCircle) * (friendlat - latCircle) +
            (friendlong - longCircle) * (friendlong - longCircle) <= 10 * 10)
            return true
        else
            return false

    }

    /**
     *  This method is called within searchFriends method where it takes the
     *  FriendId from the database using the Users ID and gets the friends object.
     *
     */
    private fun getUser(friendId: String, geofenceLatT: String, geofenceLongT: String, myCallback: MyCallback) {

        database.orderByChild("id")
            .equalTo(friendId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    if (dataSnapshot.value != null) {
                        dataSnapshot.children.forEach {

                            var user: User
                            var emailAddress: String = it.child("emailAddress").getValue().toString()
                            var longT: String = it.child("longT").getValue().toString()
                            var latT: String = it.child("latT").getValue().toString()
                            var name: String = it.child("name").getValue().toString()

                            user = User(name, emailAddress, friendId, latT, longT, LocalDateTime.now())
                            myCallback.onCallback(user, geofenceLatT, geofenceLongT)
                            Log.d("getUser", user.emailAddress)
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {

                }
            })


    }


    /**
     *  This method creates a concatenated string of all friends (1 or more)
     *  of a user and add them to an intent. The intent then opens the mapsActivity
     *  and passes teh data to the required methods to display markers on those
     *  friends location.
     */
    private fun getAllFriendsLocation() {
        if (userList.isNotEmpty() && userList[0].emailAddress != "" && userList[0].emailAddress != null) {
            val intent = Intent(this, MapsActivity::class.java)
            var tempEmail: String = ""
            var tempLatT: String = ""
            var tempLongT: String = ""
            var counter: Int = 0
            for (i in 0 until userList.count()) {
                if (counter != 0) {
                    tempEmail += "~"
                    tempLatT += "~"
                    tempLongT += "~"
                }
                tempEmail += userList[i].emailAddress
                tempLatT += userList[i].latT
                tempLongT += userList[i].longT

                counter++


            }
            intent.putExtra("emailAddress", tempEmail)
            intent.putExtra("longT", tempLongT)
            intent.putExtra("latT", tempLatT)
            startActivity(intent)
        }
    }

}






