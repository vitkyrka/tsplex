package `in`.rab.tsplex

import `in`.rab.tsplex.TestHelper.Companion.clickExampleSearch
import `in`.rab.tsplex.TestHelper.Companion.clickPreview
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@LargeTest
class AutoSearchTest {

    @get:Rule
    var activityScenarioRule = ActivityScenarioRule<SearchActivity>(SearchActivity::class.java)

    private lateinit var mSearchResource: IdlingResource
    private lateinit var mVideoResource: IdlingResource

    @Before
    fun registerIdlingResource() {
        activityScenarioRule.scenario.onActivity {
            mSearchResource = it.mIdleResource
            mVideoResource = it.mVideoFetchResource

            IdlingRegistry.getInstance().register(mSearchResource)
            IdlingRegistry.getInstance().register(mVideoResource)
        }
    }


    @After
    fun unregisterIdlingResource() {
        mSearchResource?.let {
            IdlingRegistry.getInstance().unregister(it)
        }

        mVideoResource?.let {
            IdlingRegistry.getInstance().unregister(it)
        }
    }

    @Test
    fun searchHistoryOnSignPreview() {
        onView(withId(R.id.searchView))
                .perform(typeText("444"), closeSoftKeyboard())
        onView(withId(R.id.list))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0,
                        clickPreview()))
        onView(withId(R.id.clearSearchBox))
                .perform(click())
        onView(withText("444")).check(matches(isDisplayed()))
    }

    @Test
    fun searchHistoryOnSignClick() {
        onView(withId(R.id.searchView))
                .perform(typeText("445"), closeSoftKeyboard())
        onView(withId(R.id.list))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0,
                        click()))

        Espresso.pressBack()

        onView(withId(R.id.clearSearchBox))
                .perform(click())
        onView(withText("445")).check(matches(isDisplayed()))
    }

    @Test
    fun searchHistoryOnExamplePreview() {
        onView(withId(R.id.searchView))
                .perform(typeText("zlat"), closeSoftKeyboard())
        onView(withId(R.id.list))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1,
                        clickPreview()))
        onView(withId(R.id.clearSearchBox))
                .perform(click())
        onView(withText("zlat")).check(matches(isDisplayed()))
    }

    @Test
    fun searchHistoryOnExampleClick() {
        onView(withId(R.id.searchView))
                .perform(typeText("zla"), closeSoftKeyboard())
        onView(withId(R.id.list))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1,
                        click()))
        Espresso.pressBack()
        onView(withId(R.id.clearSearchBox))
                .perform(click())
        onView(withText("zla")).check(matches(isDisplayed()))
    }

    @Test
    fun searchHistoryOnExampleSearchClick() {
        onView(withId(R.id.searchView))
                .perform(typeText("zlata"), closeSoftKeyboard())
        onView(withId(R.id.list))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1,
                        clickExampleSearch()))
        Espresso.pressBack()
        onView(withId(R.id.clearSearchBox))
                .perform(click())
        onView(withText("zlata")).check(matches(isDisplayed()))
    }
}