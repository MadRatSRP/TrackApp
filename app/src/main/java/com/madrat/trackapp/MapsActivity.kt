package com.madrat.trackapp

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions


class MapsActivity : AppCompatActivity() {
    companion object {
        private const val PERMISSIONS_REQUEST = 100

        private const val INTENT_GET_USER_CURRENT_LOCATION = 1;
        const val PARAM_PENDING_INTENT = "pendingIntent"
        const val PARAM_RESULT = "result"

        private const val SERVICE_RETURN_LAT_LNG = "SERVICE_RETURN_LAT_LNG"

        public const val STATUS_FINISH = 200;
    }

    private var mapFragment: SupportMapFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment

        //Check whether GPS tracking is enabled//
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            finish()
        }
        //Check whether this app has access to the location permission//
        val permission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        //If the location permission has been granted, then start the TrackerService//
        if (permission == PackageManager.PERMISSION_GRANTED) {
            startTrackerService()
        } else {
            //If the app doesn’t currently have access to the user’s location, then request access//
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //If the permission has been granted...//
        if (requestCode == PERMISSIONS_REQUEST && grantResults.size == 1
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //...then start the GPS tracking service//
            startTrackerService()
        } else {
            //If the user denies the permission request, then display a toast with some more information//
            Toast.makeText(
                this,
                "Please enable location services to allow GPS tracking",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    //Start the TrackerService//
    private fun startTrackerService() {
        val curIntent = Intent()

        val pendingIntent = createPendingResult(
            INTENT_GET_USER_CURRENT_LOCATION, curIntent, 0
        )

        val intent = Intent(this, TrackingService::class.java)
        intent.putExtra(PARAM_PENDING_INTENT, pendingIntent)

        startService(intent)

        /*val intent = Intent(SERVICE_RETURN_LAT_LNG)

        intent.setClass(this, )

        intent.putExtra("dev", 22f)

        sendBroadcast(intent)*/

        //Notify the user that tracking has been enabled//
        Toast.makeText(this, "GPS tracking enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val result: Bundle? = data?.getParcelableExtra(
            PARAM_RESULT
        )
        val latLng: LatLng? = result?.getParcelable("latLng")

        if (resultCode == STATUS_FINISH) {
            when (requestCode) {
                INTENT_GET_USER_CURRENT_LOCATION -> {
                    mapFragment?.getMapAsync {googleMap->
                        googleMap.addMarker(
                            latLng?.let {
                                MarkerOptions()
                                    .icon(ResourcesCompat.getDrawable(resources,
                                        R.drawable.user_icon, null)?.let { icon ->
                                        formMarkerIconFromDrawable(icon)
                                    })
                                    .position(it)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun formMarkerIconFromDrawable(customMarkerDrawable: Drawable)
            : BitmapDescriptor {
        val canvas = Canvas()
        val bitmap = Bitmap.createBitmap(customMarkerDrawable.intrinsicWidth,
            customMarkerDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        canvas.setBitmap(bitmap)

        customMarkerDrawable.setBounds(0, 0, customMarkerDrawable.intrinsicWidth,
            customMarkerDrawable.intrinsicHeight)
        customMarkerDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    override fun onDestroy() {
        super.onDestroy()

    }
}