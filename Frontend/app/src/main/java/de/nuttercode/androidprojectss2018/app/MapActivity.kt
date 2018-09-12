package de.nuttercode.androidprojectss2018.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.*
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import com.google.android.gms.maps.CameraUpdateFactory

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import de.nuttercode.androidprojectss2018.csi.ClientConfiguration
import de.nuttercode.androidprojectss2018.csi.EventStore
import de.nuttercode.androidprojectss2018.csi.ScoredEvent
import de.nuttercode.androidprojectss2018.csi.TagStore

class MapActivity : AppCompatActivity(), OnMapReadyCallback, EventListFragment.OnListFragmentInteractionListener {

    private lateinit var mMap: GoogleMap
    private lateinit var mList: EventListFragment
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var clientConfig: ClientConfiguration
    private lateinit var eventStore: EventStore

    private var firstStart = true

    private var newEventsAvailable = false

    private var notifId = 0

    private lateinit var jobScheduler: JobScheduler
    private var jobId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel()

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

        firstStart = sharedPrefs.getBoolean(SHARED_PREFS_FIRST_START, true)

        if (firstStart) {
            // If this is indeed the first start, we need to create new entries in SharedPreferences
            val freshClientConfiguration = ClientConfiguration().apply {
                radius = 200.0 // TODO: Get radius from settings
            }
            saveToSharedPrefs(SHARED_PREFS_CLIENT_CONFIG, freshClientConfiguration)

            // Remember that we are not on the first start anymore
            sharedPrefs.edit().putBoolean(SHARED_PREFS_FIRST_START, false).apply()
        }

        // At this point, there must be a ClientConfiguration saved --> retrieve it
        clientConfig = Gson().fromJson(sharedPrefs.getString(SHARED_PREFS_CLIENT_CONFIG, null), ClientConfiguration::class.java)
        eventStore = EventStore(clientConfig)
        saveToSharedPrefs(SHARED_PREFS_EVENT_STORE, eventStore)

        setContentView(R.layout.activity_map)

        // Obtain the Event list
        mList = supportFragmentManager.findFragmentById(R.id.list) as EventListFragment

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Get events/tags for the first time and update the view
        val updateEventsTask = object : UpdateEventsTask(this@MapActivity) {
            override fun onPostExecute(result: Boolean) {
                updateEventList()
                updateEventMap()
            }
        }

        val updateTagsTask = object : UpdateTagsTask(this@MapActivity) {
            override fun onPostExecute(result: Boolean) {
                val tagStore = Gson().fromJson(getFromSharedPrefs(SHARED_PREFS_TAG_STORE), TagStore::class.java)
                for (t in tagStore.all) clientConfig.tagPreferenceConfiguration.addTag(t)
                saveToSharedPrefs(SHARED_PREFS_CLIENT_CONFIG, clientConfig)
                updateEventsTask.execute()
            }
        }
        updateTagsTask.execute()

        // Schedule the repeating tasks for event and tag fetching
        jobScheduler.schedule(buildJobInfo(PERIODIC_EVENT_UPDATES_JOB_ID, UpdateEventsJobService::class.java))
        jobScheduler.schedule(buildJobInfo(PERIODIC_TAG_UPDATES_JOB_ID, UpdateTagsJobService::class.java))
    }

    override fun onResume() {
        super.onResume()
        // If the user opens the activity, he likely wants to see the most recent data
        if (newEventsAvailable) {
            updateEventList()
            updateEventMap()
        }
    }

    override fun onListFragmentInteraction(item: ScoredEvent?) {
        // Create an Intent for that specific event and start the overview activity
        val intent = Intent(this, EventOverviewActivity::class.java)
        intent.putExtra(EXTRA_EVENT_CLICKED, item)
        startActivity(intent)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
    }

    fun saveToSharedPrefs(key: String, value: Any) {
        val internalPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        internalPrefs.edit().putString(key, Gson().toJson(value)).apply()
    }

    fun getFromSharedPrefs(key: String): String {
        val internalSharedPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        return internalSharedPrefs.getString(key, null)
                ?: throw IllegalStateException("SharedPreferences do not contain key '$key'")
    }

    fun updateEventList() {
        val mostRecentEventStore = Gson().fromJson(getFromSharedPrefs(SHARED_PREFS_EVENT_STORE), EventStore::class.java)
        mList.clearList()
        mList.addAllElements(mostRecentEventStore.all)
        mList.refreshList()
    }

    fun updateEventMap() {
        if (!::mMap.isInitialized) return

        // Make sure to remove all markers (we will add them again if their events are still in the store)
        mMap.clear()

        val mostRecentEventStore = Gson().fromJson(getFromSharedPrefs(SHARED_PREFS_EVENT_STORE), EventStore::class.java)

        // If there are no events in the EventStore, we can quit already
        if (mostRecentEventStore.all.isEmpty()) return

        val boundsBuilder = LatLngBounds.builder()
        for (scoredEvent in mostRecentEventStore.all) {
            val venuePos = LatLng(scoredEvent.event.venue.latitude, scoredEvent.event.venue.longitude)
            boundsBuilder.include(venuePos)
            mMap.addMarker(MarkerOptions().position(venuePos).title("${scoredEvent.event.name} at ${scoredEvent.event.venue.name}"))
        }
        // Move the camera in such a way that every event marked on the map is visible
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100))
    }

    private fun buildJobInfo(id: Int, cls: Class<*>): JobInfo {
        if (cls != UpdateTagsJobService::class.java && cls != UpdateEventsJobService::class.java)
            throw IllegalArgumentException("Can only pass UpdateTagsJobService or UpdateEventsJobService")

        return JobInfo.Builder(id, ComponentName(this, cls))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(if (cls == UpdateTagsJobService::class.java) 1000L * 60 * 60 else 1000L * 60 * 15)
                .build()
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Default Channel"
            val description = "Channel for all notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance)
            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager!!.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(title: String, text: String) {
        val tmpIntent = Intent(this, MapActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) }
        val pendingIntent = PendingIntent.getActivity(this, notifId++, tmpIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notifBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon_background)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

        val notifManager = NotificationManagerCompat.from(this)
        notifManager.notify(notifId, notifBuilder.build())
    }

    companion object {
        const val TAG = "MapActivity"
    }
}