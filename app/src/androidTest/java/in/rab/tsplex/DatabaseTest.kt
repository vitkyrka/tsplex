package `in`.rab.tsplex

import android.util.Log
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
        assertThat(sign?.examples,
                hasItems(withExampleVideo(containsString("Igår åkte")),
                        withExampleVideo(containsString("Färdtjänst är bättre"))))
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

    @Test
    fun getExamples() {
        assertThat(db.getExamples("zlatan"),
                containsInAnyOrder(
                        withExampleVideo(containsString("Zlatan har publicerat")),
                        withExampleVideo(containsString("Zlatan är riktigt bra"))))

        assertThat(db.getExamples("zLaT"),
                containsInAnyOrder(
                        withExampleVideo(containsString("Zlatan har publicerat")),
                        withExampleVideo(containsString("Zlatan är riktigt bra"))))

        assertThat(db.getExamples("publi"),
                hasItem(withExampleVideo(containsString("Zlatan har publicerat"))))

        // assertThat(db.getExamples("lat"),
        //        not(hasItem(withExampleVideo(containsString("Zlatan har publicerat")))))
    }

    @Test
    fun getExamplesSigns() {
        assertThat(db.getExampleSigns("00001-fras-2"),
                contains(withWord("beställa, beställning"),
                        withWord("taxi")))
    }

    @Test
    fun favorites() {
        db.removeAllBookmarks()
        assertThat(db.getFavorites(), empty())

        db.addToFavorites(1)
        db.addToFavorites(2)
        db.addToFavorites(3)

        assertThat(db.getFavorites(),
                contains(withWord("dop, döpa"),
                        withWord("mössa"),
                        withWord("taxi")))

        db.removeFromFavorites(2)

        assertThat(db.getFavorites(),
                contains(withWord("mössa"),
                        withWord("taxi")))

        db.removeAllBookmarks()
        assertThat(db.getFavorites(), empty())
    }

    @Test
    fun history() {
        db.clearHistory()
        assertThat(db.getHistory(), empty())

        db.addToHistory(3)
        db.addToHistory(1)

        assertThat(db.getHistory(),
                contains(withId(1),
                        withId(3)))

        db.addToHistory(2)

        assertThat(db.getHistory(),
                contains(withId(2),
                        withId(1),
                        withId(3)))

        db.addToHistory(1)

        assertThat(db.getHistory(),
                contains(withId(1),
                        withId(2),
                        withId(3)))


        for (i in 100..150) {
            db.addToHistory(i)
        }

        val history = db.getHistory()
        assertThat(history, hasSize(50))
        assertThat(history, hasItem((withId(150))))
        assertThat(history, not(hasItem((withId(1)))))

        db.clearHistory()
        assertThat(db.getHistory(), empty())
    }

    private fun withWord(matcher: Matcher<String>): Matcher<Sign> {
        return object : TypeSafeMatcher<Sign>() {
            override fun matchesSafely(sign: Sign): Boolean {
                return matcher.matches(sign.word)
            }

            override fun describeTo(description: Description) {
                description.appendText("with word: ")
                matcher.describeTo(description)
            }
        }
    }

    private fun withWord(word: String) = withWord(equalTo(word))

    private fun withExampleVideo(matcher: Matcher<String>): Matcher<Example> {
        return object : TypeSafeMatcher<Example>() {
            override fun matchesSafely(example: Example): Boolean {
                return matcher.matches(example.toString())
            }

            override fun describeTo(description: Description) {
                description.appendText("with video: ")
                matcher.describeTo(description)
            }
        }
    }

    private fun withExampleVideo(video: String) = withExampleVideo(equalTo(video))

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