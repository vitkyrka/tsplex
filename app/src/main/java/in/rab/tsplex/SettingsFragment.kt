package `in`.rab.tsplex

import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.SearchRecentSuggestions
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import kotlinx.android.synthetic.main.fragment_sign_list.*

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference("licenses").setOnPreferenceClickListener {
            OssLicensesMenuActivity.setActivityTitle(getString(R.string.open_source_licenses))
            startActivity(Intent(context, OssLicensesMenuActivity::class.java))
            return@setOnPreferenceClickListener true
        }

        findPreference("clearSearchHistory").setOnPreferenceClickListener {
            SearchRecentSuggestions(context,
                    SignRecentSuggestionsProvider.AUTHORITY, SignRecentSuggestionsProvider.MODE)
                    .clearHistory()
            return@setOnPreferenceClickListener true
        }

        findPreference("clearSignHistory").setOnPreferenceClickListener {
            ClearSignHistoryTask().execute()
            return@setOnPreferenceClickListener true
        }

        findPreference("removeAllBookmarks").setOnPreferenceClickListener {
            RemoveAllBookmarksTask().execute()
            return@setOnPreferenceClickListener true
        }

        findPreference("about").setOnPreferenceClickListener {
            val pack = activity?.packageName ?: return@setOnPreferenceClickListener true
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(
                        "https://play.google.com/store/apps/details?id=$pack")
                setPackage("com.android.vending")
            }
            startActivity(intent)
            return@setOnPreferenceClickListener true
        }
    }

    private inner class ClearSignHistoryTask : AsyncTask<Void, Void, Void>() {
        override fun doInBackground(vararg params: Void): Void? {
            activity?.let {
                SignDatabase(it).clearHistory()
            }
            return null
        }
    }

    private inner class RemoveAllBookmarksTask : AsyncTask<Void, Void, Void>() {
        override fun doInBackground(vararg params: Void): Void? {
            activity?.let {
                SignDatabase(it).removeAllBookmarks()
            }
            return null
        }
    }
}