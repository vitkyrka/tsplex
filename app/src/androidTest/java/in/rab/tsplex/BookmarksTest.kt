package `in`.rab.tsplex

import androidx.recyclerview.widget.RecyclerView
import androidx.test.InstrumentationRegistry
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@LargeTest
class BookmarksTest {

    @get:Rule
    var activityScenarioRule = ActivityScenarioRule<HomeActivity>(HomeActivity::class.java)

    private fun toggleSignBookmark(signId: Int) {
        onView(withId(R.id.searchView))
                .perform(typeText("%02d".format(signId)), pressImeActionButton())
        onView(withId(R.id.list))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1,
                        click()))
        onView(withId(R.id.menu_star))
                .perform(click())
    }

    private fun removeAllBookmarks() {
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext())
        onView(withText(R.string.settings))
                .perform(click())
        onView(withText(R.string.remove_all_bookmarks))
                .perform((click()))
        Espresso.pressBack()
    }

    @Test
    fun bookmarksTest() {
        removeAllBookmarks()

        onView(withId(R.id.homeSearchView))
                .perform(click())

        for (i in 1..2) {
            if (i > 1) {
                onView(withId(R.id.search))
                        .perform(click())
            }

            toggleSignBookmark(i)
        }

        onView(withContentDescription(R.string.abc_action_bar_up_description))
                .perform(click())

        onView(withId(R.id.navigation_favorites))
                .perform((click()))

        onView(withText("taxi")).check(matches(isDisplayed()))
        onView(withText("dop, döpa")).check(matches(isDisplayed()))

        onView(withId(R.id.list))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1,
                        click()))
        onView(withId(R.id.menu_star))
                .perform(click())
        Espresso.pressBack()

        onView(withText("taxi")).check(matches(isDisplayed()))
        onView(withText("dop, döpa")).check(doesNotExist())

        removeAllBookmarks()

        onView(withText("taxi")).check(doesNotExist())
    }

    private fun addFolder(name: String) {
        onView(withId(R.id.menu_add_folder))
                .perform((click()))
        onView(withHint("Namn"))
                .perform(typeText(name))
        onView(withText("OK"))
                .perform(click())
        onView(withText(name))
                .check(matches(isDisplayed()))
    }

    @Test
    fun foldersTest() {
        removeAllBookmarks()

        onView(withId(R.id.navigation_favorites))
                .perform((click()))

        onView(withId(R.id.menu_add_folder))
                .perform((click()))
        onView(withHint("Namn"))
                .perform(typeText("foo"))
        onView(withText("Avbryt"))
                .perform(click())
        onView(withText("foo"))
                .check(doesNotExist())

        addFolder("foo")

        onView(withId(R.id.homeSearchView))
                .perform(click())
        toggleSignBookmark(1)
        onView(withText("foo"))
                .perform(click())

        onView(withContentDescription(R.string.abc_action_bar_up_description))
                .perform(click())
        onView(withId(R.id.navigation_favorites))
                .perform((click()))

        onView(withText("taxi"))
                .check(matches(isDisplayed()))
        onView(withText("foo"))
                .perform(click())
        onView(withText("taxi"))
                .check(matches(isDisplayed()))

        onView(withId(R.id.search))
                .perform(click())
        toggleSignBookmark(2)
        onView(withText("foo"))
                .perform(click())
        Espresso.pressBack()
        onView(withText("dop, döpa"))
                .check(matches(isDisplayed()))

        onView(withContentDescription(R.string.abc_action_bar_up_description))
                .perform(click())

        addFolder("bar")
        onView(withId(R.id.homeSearchView))
                .perform(click())
        toggleSignBookmark(3)
        onView(withText("bar"))
                .perform(click())

        onView(withContentDescription(R.string.abc_action_bar_up_description))
                .perform(click())

        onView(withText("bar"))
                .perform(click())
        onView(withText("mössa"))
                .check(matches(isDisplayed()))
        Espresso.pressBack()

        onView(withText("bar"))
                .perform(longClick())
        onView(withText("Nej"))
                .perform(click())
        onView(withText("mössa"))
                .check(matches(isDisplayed()))

        onView(withText("bar"))
                .perform(longClick())
        onView(withText("Ja"))
                .perform(click())
        onView(withText("taxi"))
                .check(matches(isDisplayed()))
        onView(withText("mössa"))
                .check(doesNotExist())

        onView(withText("foo"))
                .perform(longClick())
        onView(withText("Ja"))
                .perform(click())
        onView(withText("taxi"))
                .check(doesNotExist())

        onView(withId(R.id.homeSearchView))
                .perform(click())
        toggleSignBookmark(1)
        onView(withContentDescription(R.string.abc_action_bar_up_description))
                .perform(click())
        onView(withText("taxi"))
                .check(matches(isDisplayed()))
    }
}