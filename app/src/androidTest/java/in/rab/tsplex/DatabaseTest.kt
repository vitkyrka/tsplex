package `in`.rab.tsplex

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
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
        db = SignDatabase.getInstance(ApplicationProvider.getApplicationContext())
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun getSign() {
        val sign = db.getSign(5705)
        assertThat(sign?.word, equalTo("hejare på, strong, kan, en baddare på"))
        assertThat(sign?.examples,
                hasItems(withExampleVideo(containsString("Zlatan är riktigt bra")),
                        withExampleVideo(containsString("Min kusin är"))))
    }

    @Test
    fun getSynonyms() {
        assertThat(db.getSynonyms(1),
                contains(withId(1728),
                        withId(620),
                        withId(348)))

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

        assertThat(db.getExamples("lat"),
                not(hasItem(withExampleVideo(containsString("Zlatan har publicerat")))))
    }

    @Test
    fun getRandomExamples() {
        val one = db.getRandomExamples()
        val two = db.getRandomExamples()

        assertThat(db.getExampleSigns(one[0].video),
                hasSize(greaterThanOrEqualTo(2)))

        assertThat(one, not(empty()))
        assertThat(one, not(equalTo(two)))
    }

    @Test
    fun getExamplesSigns() {
        assertThat(db.getExampleSigns("00001-fras-2"),
                containsInAnyOrder(
                        withWord("jag"),
                        withWord("har gjort, har varit"),
                        withWord("beställa, beställning"),
                        withWord("taxi")))
    }

    @Test
    fun getTopics() {
        assertThat(db.getTopics("jurid"),
                contains(withName("Juridik"),
                        withName("Vad är juridik")))

        assertThat(db.getTopics("harry po"),
                contains(withName("Harry Potter")))
    }

    @Test
    fun getSubTopics() {
        assertThat(db.getSubTopics(0x00000001),
                hasItem(withName("Film & Böcker")))
        assertThat(db.getSubTopics(0x00000001),
                not(hasItem(withTopicId(0x00000001))))
        assertThat(db.getSubTopics(0x00000001),
                not(hasItem(withName(("Harry Potter")))))
    }

    @Test
    fun getParentTopic() {
        assertThat(db.getParentTopic(0x00000001),
                hasSize(0))
        assertThat(db.getParentTopic(0x00000101),
                contains(withTopicId(0x00000001)))
        assertThat(db.getParentTopic(0x0502090d),
                contains(withTopicId(0x0002090d)))
    }

    @Test
    fun favorites() {
        db.removeAllBookmarks()
        assertThat(db.getFavorites(), empty())

        db.addToFavorites(1, 0)
        db.addToFavorites(2, 0)
        db.addToFavorites(3, 0)

        assertThat(db.isFavorite(2), equalTo(true))

        assertThat(db.getFavorites(),
                contains(withWord("dop, döpa"),
                        withWord("mössa"),
                        withWord("taxi")))

        db.removeFromFavorites(2)
        assertThat(db.isFavorite(2), equalTo(false))

        assertThat(db.getFavorites(),
                contains(withWord("mössa"),
                        withWord("taxi")))

        db.removeAllBookmarks()
        assertThat(db.getFavorites(), empty())

        assertThat(db.isFavorite(1), equalTo(false))
    }

    @Test
    fun folders() {
        db.removeAllBookmarks()
        assertThat(db.getBookmarksFolders(), empty())

        db.addBookmarksFolder("c")
        db.addBookmarksFolder("a")
        db.addBookmarksFolder("b")

        val folders = db.getBookmarksFolders()

        assertThat(folders, contains(
                withFolderName("a"),
                withFolderName("b"),
                withFolderName("c")
        ))

        db.addToFavorites(1, folders[0].id)
        db.addToFavorites(2, folders[0].id)
        db.addToFavorites(3, folders[2].id)

        assertThat(db.getFolderSigns(folders[0].id),
                contains(withWord("dop, döpa"),
                        withWord("taxi")))
        assertThat(db.getFolderSigns(folders[2].id),
                contains(withWord("mössa")))

        assertThat(db.getFavorites(),
                contains(withWord("dop, döpa"),
                        withWord("mössa"),
                        withWord("taxi")))

        db.addToFavorites(4, 0)

        db.removeBookmarksFolder(folders[0].id)
        assertThat(db.getFavorites(),
                contains(withWord("Indien"),
                        withWord("mössa")))

        assertThat(db.getFolderSigns(folders[0].id), empty())
        assertThat(db.getFolderSigns(folders[1].id), empty())
        assertThat(db.getFolderSigns(folders[2].id), not(empty()))

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

    private fun withTopicId(id: Long): Matcher<Topic> {
        return object : TypeSafeMatcher<Topic>() {
            override fun matchesSafely(topic: Topic): Boolean {
                return topic.id == id
            }

            override fun describeTo(description: Description) {
                description.appendText("with id: ")
                description.appendValue(id)
            }
        }
    }

    private fun withName(matcher: Matcher<String>): Matcher<Topic> {
        return object : TypeSafeMatcher<Topic>() {
            override fun matchesSafely(topic: Topic): Boolean {
                return matcher.matches(topic.name)
            }

            override fun describeTo(description: Description) {
                description.appendText("with name: ")
                matcher.describeTo(description)
            }
        }
    }

    private fun withName(name: String) = withName(equalTo(name))

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

    private fun withFolderName(matcher: Matcher<String>): Matcher<Folder> {
        return object : TypeSafeMatcher<Folder>() {
            override fun matchesSafely(folder: Folder): Boolean {
                return matcher.matches(folder.name)
            }

            override fun describeTo(description: Description) {
                description.appendText("with name: ")
                matcher.describeTo(description)
            }
        }
    }

    private fun withFolderName(name: String) = withFolderName(equalTo(name))
}