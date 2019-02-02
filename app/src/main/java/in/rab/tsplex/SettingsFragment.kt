package `in`.rab.tsplex

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.SearchRecentSuggestions
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference("licenses").setOnPreferenceClickListener {
            OssLicensesMenuActivity.setActivityTitle(getString(R.string.open_source_licenses))
            startActivity(Intent(context, OssLicensesMenuActivity::class.java))
            return@setOnPreferenceClickListener true
        }

        findPreference("clearHistory").setOnPreferenceClickListener {
            SearchRecentSuggestions(context,
                    SignRecentSuggestionsProvider.AUTHORITY, SignRecentSuggestionsProvider.MODE)
                    .clearHistory()
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
}