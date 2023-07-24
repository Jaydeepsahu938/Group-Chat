package com.example.mychatapp

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService:FirebaseMessagingService() {

    companion object{
       const val TAG="MyFirebseMessging"
    }
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG,"FCM message Id: ${message!!.messageId}")
        Log.d(TAG,"FCM Notification Message: ${message.notification}")
        Log.d(TAG,"FCM date Message:${message.data}")
    }
}