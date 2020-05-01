package `in`.rab.tsplex

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_home.*

class HomeActivity : RoutingAppCompactActivity(), BottomNavigationView.OnNavigationItemSelectedListener {
    private var mOrdboken: Ordboken? = null
    private var mTitle: String? = null

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString("title", mTitle)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        mOrdboken = Ordboken.getInstance(this)

        setContentView(R.layout.activity_home)
        setSupportActionBar(toolbar)

        supportActionBar?.title = null

        navigation.setOnNavigationItemSelectedListener(this)

        val openSearch = View.OnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        findViewById<View>(R.id.toolbarIcon).setOnClickListener(openSearch)
        findViewById<View>(R.id.toolbarTitle).setOnClickListener(openSearch)

        if (savedInstanceState != null) {
            mTitle = savedInstanceState.getString("title")
        }

        if (mTitle == null) {
            onNavigationItemSelected(navigation.menu.findItem(R.id.navigation_home))
        }
    }

    private fun setAndSaveTitle(res: Int) {
        mTitle = getString(res)
    }

    override fun onNavigationItemSelected(it: MenuItem): Boolean {
        it.isChecked = true

        when (it.itemId) {
            R.id.navigation_home -> {
                val fragment: androidx.fragment.app.Fragment? = HomeFragment.newInstance()
                supportFragmentManager.beginTransaction().replace(R.id.content, fragment!!, "home").commit()
                setAndSaveTitle(R.string.app_name)
                return true
            }
            R.id.navigation_history -> {
                val fragment: androidx.fragment.app.Fragment? = HistoryFragment.newInstance()
                supportFragmentManager.beginTransaction().replace(R.id.content, fragment!!, "history").commit()
                setAndSaveTitle(R.string.title_history)
                return true
            }
            R.id.navigation_favorites -> {
                val fragment: androidx.fragment.app.Fragment? = FavoritesFragment.newInstance()
                supportFragmentManager.beginTransaction().replace(R.id.content, fragment!!, "favorites").commit()
                setAndSaveTitle(R.string.title_favorites)
                return true
            }
            else -> {
                return false
            }
        }
    }

    override fun onItemLongClick(item: Folder): Boolean {
        val builder = AlertDialog.Builder(this)

        builder.setTitle(R.string.delete_folder)
                .setMessage(getString(R.string.delete_folder_x, item.name))
                .setPositiveButton(R.string.yes) { _, _ ->
                    SignDatabase.getInstance(this).removeBookmarksFolder(item.id)

                    supportFragmentManager.findFragmentByTag("favorites")?.also {
                        supportFragmentManager.beginTransaction().apply {
                            detach(it)
                            attach(it)
                            commit()
                        }
                    }
                }
                .setNegativeButton(R.string.no) { dialog, _ ->
                    dialog.cancel()
                }
                .show()

        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.home, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (mOrdboken!!.onOptionsItemSelected(this, item)) {
            return true
        }

        if (item?.itemId == R.id.settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        }


        return super.onOptionsItemSelected(item)
    }
}
