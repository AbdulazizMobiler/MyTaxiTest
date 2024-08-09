package uz.mobiledev.mytaxitest.tools

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uz.mobiledev.data.model.Location
import uz.mobiledev.data.repository.LocationRepository
import uz.mobiledev.mytaxitest.MainActivity
import uz.mobiledev.mytaxitest.R
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class LocationForegroundService: Service() {

    @Inject
    lateinit var locationRepository: LocationRepository
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationInterval = 1

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }


    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            val location = locationResult.lastLocation
            Log.d(TAG, "Latitude: ${location?.latitude}, Longitude: ${location?.longitude}")
            location?.let {
                CoroutineScope(Dispatchers.IO).launch {
                    locationRepository.saveLocation(
                        Location(
                            latitude = it.latitude,
                            longitude = it.longitude,
                            accuracy = it.accuracy.toDouble(),
                            createdTime = Calendar.getInstance().timeInMillis
                        )
                    )
                }
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = createNotification(pendingIntent)
        startForeground(ONGOING_NOTIFICATION_ID, notification)
        requestLocationUpdates()
        return START_STICKY
    }

    private fun requestLocationUpdates() {
        try {
            CoroutineScope(Dispatchers.Main).launch {
                val locationRequest: LocationRequest = LocationRequest.create().apply {
                    interval = locationInterval.toLong() * 60 * 1000
                    fastestInterval = locationInterval.toLong() * 60 * 1000
                    priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
                }
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    null
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Lost location permission. Could not request updates. $e")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun createNotification(pendingIntent: PendingIntent?): Notification {
        return NotificationCompat.Builder(this, "location")
            .setContentTitle(getString(R.string.title_gps))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "location",
                "Location Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }


    companion object {
        private const val TAG = "LocationService"
        private const val ONGOING_NOTIFICATION_ID = 12345
    }
}