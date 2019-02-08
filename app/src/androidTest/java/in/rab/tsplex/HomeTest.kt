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
class HomeTest {

    @get:Rule
    var activityScenarioRule = ActivityScenarioRule<HomeActivity>(HomeActivity::class.java)

    private fun addToHistory(signId: Int) {
        onView(withId(R.id.searchView))
                .perform(typeText("%02d".format(signId)), pressImeActionButton())
        onView(withId(R.id.list))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0,
                        click()))
    }

    @Test
    fun historyTest() {
        onView(withId(R.id.homeSearchView))
                .perform(click())

        for (i in 1..2) {
            if (i > 1) {
                onView(withId(R.id.search))
                        .perform(click())
            }

            addToHistory(i)
        }

        onView(withContentDescription(R.string.abc_action_bar_up_description))
                .perform(click())

        onView(withId(R.id.navigation_home))
                .perform((click()))


        onView(withText("dop, döpa")).check(matches(isDisplayed()))
        onView(withText("taxi")).check(matches(isDisplayed()))

        onView(withId(R.id.homeSearchView))
                .perform(click())
        addToHistory(3)

        onView(withContentDescription(R.string.abc_action_bar_up_description))
                .perform(click())

        onView(withText("dop, döpa")).check(matches(isDisplayed()))
        onView(withText("mössa")).check(matches(isDisplayed()))
    }
}