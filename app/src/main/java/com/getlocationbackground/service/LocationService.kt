package com.getlocationbackground.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.getlocationbackground.receiver.RestartBackgroundService
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.stylish.spacex.data.model.Trip
import java.util.*


class LocationService : Service() {
    var counter = 0
    var latitude: Double = 0.0
    var longitude: Double = 0.0
    var accuracy: Float = 0.0F
    var timeStamp:Long = 0
    private val TAG = "LocationService"

  lateinit var  sharedPreferences: SharedPreferences
  val gson = Gson()
    override fun onCreate() {
        super.onCreate()

      sharedPreferences = this.getSharedPreferences("my_share_pref",Context.MODE_PRIVATE)

      if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) createNotificationChanel() else startForeground(
            1,
            Notification()
        )

        requestLocationUpdates()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChanel() {
        val NOTIFICATION_CHANNEL_ID = "com.getlocationbackground"
        val channelName = "Background Service"
        val chan = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val manager =
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        manager.createNotificationChannel(chan)
        val notificationBuilder =
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        val notification: Notification = notificationBuilder.setOngoing(true)
            .setContentTitle("App is running count::" + counter)
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(2, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startTimer()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stoptimertask()
      if(sharedPreferences.getBoolean("is_started",false)){
        val broadcastIntent = Intent()
        broadcastIntent.action = "restartservice"
        broadcastIntent.setClass(this, RestartBackgroundService::class.java)
        this.sendBroadcast(broadcastIntent)
      }

    }

    private var timer: Timer? = null
    private var timerTask: TimerTask? = null
    fun startTimer() {
        timer = Timer()
        timerTask = object : TimerTask() {
            override fun run() {
                var count = counter++
                if (latitude != 0.0 && longitude != 0.0) {
                    Log.d(
                        "Location::",
                        latitude.toString() + ":::" + longitude.toString() + "Count" +
                                count.toString()
                    )

                  val tripId:Int = sharedPreferences.getInt("trip_id",0)

                  if(tripId != 0) {

                    val trip: String? = sharedPreferences.getString("trip" + tripId, "")

                    Log.v("Trip Details:", "trip" + trip)
                    if (trip.equals("")) {
                      val list = arrayListOf<com.stylish.spacex.data.model.Location>()
                      val location: com.stylish.spacex.data.model.Location =
                        com.stylish.spacex.data.model.Location(
                          accuracy = accuracy,
                          latitude = latitude,
                          longitide = longitude,
                          timestamp = timeStamp.toString()

                        )
                      list.add(location)
                      val trip: Trip = Trip(
                        start_time = Date().toString(),
                        locations = list,
                        end_time = "",
                        trip_id = tripId
                      )


                      val tripToString: String = gson.toJson(trip)

                      val editor: SharedPreferences.Editor = sharedPreferences.edit()
                      editor.putString("trip" + tripId, tripToString).apply()

                    } else {
                      val _trip: Trip = gson.fromJson(trip, Trip::class.java)

                      val location: com.stylish.spacex.data.model.Location =
                        com.stylish.spacex.data.model.Location(
                          accuracy = accuracy,
                          latitude = latitude,
                          longitide = longitude,
                          timestamp = timeStamp.toString()

                        )
                      _trip.locations.add(location)
                      val tripToString: String = gson.toJson(_trip)
                      val editor: SharedPreferences.Editor = sharedPreferences.edit()
                      editor.putString("trip" + tripId, tripToString).apply()
                      editor.commit()

                    }
                  }
                }
            }
        }
        timer!!.schedule(
            timerTask,
            0,
            5000
        ) //1 * 60 * 1000 1 minute
    }

    fun stoptimertask() {
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun requestLocationUpdates() {
        val request = LocationRequest()
        request.setInterval(10000)
        request.setFastestInterval(5000)
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        val client: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this)

        val permission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (permission == PackageManager.PERMISSION_GRANTED) { // Request location updates and when an update is
            // received, store the location in Firebase
            client.requestLocationUpdates(request, object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location: Location = locationResult.getLastLocation()
                    if (location != null) {
                        latitude = location.latitude
                        longitude = location.longitude
                        accuracy = location.accuracy
                        timeStamp = location.time

                        Log.d("Location Service", "location update $location")
                    }
                }
            }, null)
        }
    }
}
