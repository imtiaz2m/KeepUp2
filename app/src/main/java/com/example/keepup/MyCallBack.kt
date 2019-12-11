package com.example.keepup

import com.example.keepup.model.User

interface MyCallback {
    fun onCallback(value: User, geoLatT:String, geoLongT:String)
}