package `in`.rab.tsplex

import android.app.Activity
import android.app.TaskStackBuilder
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import kotlinx.android.synthetic.main.activity_home.*

class SettingsActivity : AppCompatActivity() {
    private lateinit var mOrdboken: Ordboken

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mOrdboken = Ordboken.getInstance(this)

        setContentView(R.layout.activity_settings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this)
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}
