package `in`.rab.tsplex

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.*
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.TypeSafeMatcher
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

    @Test
    fun getSynonyms() {
        assertThat(db.getSynonyms(1),
                contains(withId(348),
                        withId(620),
                        withId(1728)))

        assertThat(db.getSynonyms(12345), empty())
    }

    @Test
    fun getHomonyms() {
        assertThat(db.getHomonyms(3),
                contains(withId(10610),
                        withId(13144)))

        assertThat(db.getHomonyms(1), empty())
    }

    private fun withId(id: Int): Matcher<Sign> {
        return object : TypeSafeMatcher<Sign>() {
            override fun matchesSafely(sign: Sign): Boolean {
                return sign.id == id
            }

            override fun describeTo(description: Description) {
                description.appendText("with id: ")
                description.appendValue(id)
            }
        }
    }
}