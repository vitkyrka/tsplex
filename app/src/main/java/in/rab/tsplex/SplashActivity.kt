package `in`.rab.tsplex

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast


class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        DatabaseTask().execute()

        super.onCreate(savedInstanceState)

        val drawable = window.decorView.background
        ObjectAnimator.ofInt(drawable, "level", 0, 10000).apply {
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            duration = 2000
            start()
        }
    }

    private fun launchHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    private inner class DatabaseTask : AsyncTask<Void, Void, Exception?>() {
        override fun doInBackground(vararg params: Void): Exception? {
            try {
                SignDatabase(applicationContext).getDatabase()
            } catch (e: Exception) {
                return e
            }
            return null
        }

        override fun onPostExecute(e: Exception?) {
            if (e != null) {
                Toast.makeText(applicationContext, R.string.fail_database, Toast.LENGTH_LONG).show()
                finish()
            }

            launchHome()
        }
    }
}
