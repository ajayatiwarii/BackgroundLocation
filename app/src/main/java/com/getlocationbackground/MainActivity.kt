package com.getlocationbackground

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.getlocationbackground.service.LocationService
import com.getlocationbackground.util.Util
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileWriter
import java.io.IOException


class MainActivity : AppCompatActivity() {
    var mLocationService: LocationService = LocationService()
    lateinit var mServiceIntent: Intent
    lateinit var mActivity: Activity
    lateinit var sharedPreferences:SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sharedPreferences = this.getSharedPreferences("my_share_pref", Context.MODE_PRIVATE)
        mActivity = this@MainActivity


      if (!Util.isLocationEnabledOrNot(mActivity)) {
            Util.showAlertLocation(
              mActivity,
              getString(R.string.gps_enable),
              getString(R.string.please_turn_on_gps),
              getString(
                R.string.ok
              )
            )
        }

        requestPermissionsSafely(
          arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION), 200
        )


      if (!Util.isMyServiceRunning(mLocationService.javaClass, mActivity)) {
        txtStartService.text = getString(R.string.start_trip)
      } else {
        txtStartService.text = getString(R.string.end_trip)
      }

        txtStartService.setOnClickListener {
            mLocationService = LocationService()
            mServiceIntent = Intent(this, mLocationService.javaClass)
            if (!Util.isMyServiceRunning(mLocationService.javaClass, mActivity)) {
              val editor:SharedPreferences.Editor = sharedPreferences.edit()
             var tripId:Int = sharedPreferences.getInt("trip_id", 0)
              if(tripId==0){
                tripId = 1;
              }else{
                tripId++
              }
              editor.putBoolean("is_started", true)
              editor.putInt("trip_id", tripId)
              editor.apply()
                startService(mServiceIntent)
                Toast.makeText(
                  mActivity,
                  getString(R.string.service_start_successfully),
                  Toast.LENGTH_SHORT
                ).show()
              txtStartService.text = getString(R.string.end_trip)
            } else {
              val editor:SharedPreferences.Editor = sharedPreferences.edit()
              editor.putBoolean("is_started", false)
              editor.apply()

              if (::mServiceIntent.isInitialized) {
                stopService(mServiceIntent)
                txtStartService.text = getString(R.string.start_trip)

              }
            }
        }

      txt_export.setOnClickListener{
        var tripId:Int = sharedPreferences.getInt("trip_id", 0)

        save(context = this, tripId = tripId)

      }
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun requestPermissionsSafely(
      permissions: Array<String>,
      requestCode: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions!!, requestCode)
        }
    }

    override fun onDestroy() {
        if (::mServiceIntent.isInitialized) {
            stopService(mServiceIntent)
        }
        super.onDestroy()
    }

  @Throws(IOException::class)
  fun save(context: Context, tripId: Int) {
    val rootFolder: File? = context.getExternalFilesDir(null)
    val jsonFile = File(rootFolder, "file.txt")

   var tripData:String = ""

    for (i in 1..tripId) {
      val trip: String? = sharedPreferences.getString("trip" + tripId, "")
      tripData +=  trip
    }
    val writer = FileWriter(jsonFile)
    writer.write(tripData)
    writer.close()
    val fileUri = FileProvider.getUriForFile(
      context,
      context.applicationContext.packageName.toString() + ".provider",
      jsonFile
    )

    val browserIntent = Intent(Intent.ACTION_VIEW, fileUri)
    browserIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

    startActivity(browserIntent)
  }
}
