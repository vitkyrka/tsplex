package `in`.rab.tsplex

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private lateinit var db: SignDatabase

    @Before
    fun openDb() {
        db = SignDatabase(ApplicationProvider.getApplicationContext())
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun getSign() {
        val sign = db.getSign(1)
        assertThat(sign?.word, equalTo("taxi"))
    }
}