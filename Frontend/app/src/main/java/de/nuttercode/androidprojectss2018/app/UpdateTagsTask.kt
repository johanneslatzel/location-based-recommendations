package de.nuttercode.androidprojectss2018.app

import android.content.Context
import android.os.AsyncTask
import android.preference.PreferenceManager
import android.util.Log
import com.google.gson.Gson
import de.nuttercode.androidprojectss2018.csi.config.ClientConfiguration
import de.nuttercode.androidprojectss2018.csi.store.TagStore
import java.lang.ref.WeakReference

open class UpdateTagsTask(context: Context) : AsyncTask<Void, Void, Boolean>() {

    private var contextRef = WeakReference(context)
    private var sharedPrefs = PreferenceManager.getDefaultSharedPreferences(contextRef.get())

    // Get the most up-to-date TagStore from SharedPreferences or create a new one with the ClientConfiguration in SharedPreferences
    // We do not use Utils.getFromSharedPreferences() because we want to fall back to a default store if none exists yet
    private var tagStore = Gson().fromJson(sharedPrefs.getString(SHARED_PREFS_TAG_STORE, null), TagStore::class.java)
            ?: TagStore(Gson().fromJson(sharedPrefs.getString(SHARED_PREFS_CLIENT_CONFIG, null), ClientConfiguration::class.java))

    override fun doInBackground(vararg parameters: Void?): Boolean {
        val qri = tagStore.refresh()

        if (!qri.isOK) {
            Log.e(TAG, "TagQuery was not successful. Message: ${qri.message}")
            // Indicate that this job needs to be rescheduled
            return true
        }

        // Updated the TagStore in SharedPreferences
        saveToSharedPrefs(tagStore)
        // Indicate that this job does not need to be rescheduled
        return false
    }


    companion object {
        const val TAG = "UpdateTagsTask"
    }

}