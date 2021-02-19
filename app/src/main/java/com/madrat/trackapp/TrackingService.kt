package com.madrat.trackapp

import android.Manifest
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import java.lang.NullPointerException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class TrackingService() : Service() {
    companion object {
        private const val PERMISSIONS_ALLOW_USING_LOCATION_ID = 100
    }

    private val serviceTag = TrackingService::class.java.simpleName

    var executorService: ExecutorService? = null

    private var currentLocation: LatLng? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        executorService = Executors.newFixedThreadPool(2)

        buildNotification()
        requestLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pendingIntent: PendingIntent? = intent?.getParcelableExtra(MapsActivity.PARAM_PENDING_INTENT)

        println("PENDING_INTENT: $pendingIntent")
        println("CURRENT_LOCATION: $currentLocation")

        val myRun = pendingIntent?.let {
            currentLocation?.let { it1 ->
                MyRun(this, it, it1) }
        }

        try {
            executorService?.execute(myRun)
        } catch (exception: NullPointerException) {
            exception.printStackTrace()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    //Create the persistent notification//
    private fun buildNotification() {
        val stop = "stop"
        registerReceiver(stopReceiver, IntentFilter(stop))
        val broadcastIntent = PendingIntent.getBroadcast(
            applicationContext, 0, Intent(stop), PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel("my_service", "My Background Service")
        } else {
            // If earlier version channel ID is not used
            // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
            ""
        }

        val notificationBuilder = NotificationCompat.Builder(
            applicationContext, channelId
        )
        // Create the persistent notification//
        val notification = notificationBuilder
            //Make this notification ongoing so it can’t be dismissed by the user//
            .setOngoing(true)
            .setSmallIcon(R.drawable.tracking_is_enabled)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.tracking_enabled_notif))
            .setContentIntent(broadcastIntent)
            .setPriority(PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()

        startForeground(1, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String{
        val chan = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    private var stopReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            //Unregister the BroadcastReceiver when the notification is tapped//
            unregisterReceiver(this)
            //Stop the Service//
            stopSelf()
        }
    }

    //Initiate the request to track the device's location//
    private fun requestLocationUpdates() {
        val request = LocationRequest()

        //Specify how often your app should request the device’s location//
        request.interval = 10000
        //Get the most accurate location data available//
        request.priority = LocationRequest.PRIORITY_HIGH_ACCURACY


        val client: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(applicationContext)
        val permission = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        //If the app currently has access to the location permission...//
        if (permission == PackageManager.PERMISSION_GRANTED) {
            //...then request location updates//
            client.requestLocationUpdates(
                request, locationCallback, Looper.myLooper()
            )
            // looper may be nullable
        }
    }

    private val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            //Get a reference to the database, so your app can perform read and write operations//
            val location: Location = locationResult.lastLocation

            currentLocation = LatLng(
                location.latitude, location.longitude
            )
        }
    }

    class MyRun(private val context: TrackingService, private val pendingIntent: PendingIntent,
                private val currentLocation: LatLng)
        : Runnable {
        override fun run() {
            try {
                // сообщаем об окончании задачи
                val bundle = Bundle()
                bundle.putParcelable("latLng", currentLocation)

                val intent = Intent().putExtra(MapsActivity.PARAM_RESULT, bundle)

                pendingIntent.send(context, MapsActivity.STATUS_FINISH, intent)
            } catch (exception: InterruptedException) {
                exception.printStackTrace()
            } catch (exception: PendingIntent.CanceledException) {
                exception.printStackTrace()
            }
        }
    }
}
