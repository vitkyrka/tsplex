package `in`.rab.tsplex

import androidx.recyclerview.widget.RecyclerView
import androidx.test.InstrumentationRegistry
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@LargeTest
class SearchTest {

    /**
     * Use [ActivityScenarioRule] to create and launch the activity under test before each test,
     * and close it after each test. This is a replacement for
     * [androidx.test.rule.ActivityTestRule].
     */
    @get:Rule
    var activityScenarioRule = ActivityScenarioRule<SearchActivity>(SearchActivity::class.java)

    lateinit var mIdlingResource: IdlingResource

    @Before
    fun registerIdlingResource() {
        activityScenarioRule.scenario.onActivity {
            mIdlingResource = it.mIdleResource
            IdlingRegistry.getInstance().register(mIdlingResource)

            it.mAutoSearch = false
        }
    }

    @Test
    fun searchTips() {
        onView(withId(R.id.searchHelp))
                .check(matches(isDisplayed()))
                .perform(openLinkWithText("chans"))
        onView(withId(R.id.searchView))
                .check(matches(withText("chans")))
    }

    @Test
    fun searchHistory() {
        onView(withId(R.id.searchView))
                .perform(typeText("a"), pressImeActionButton())
        onView(withId(R.id.clearSearchBox))
                .perform(click())
        onView(withId(R.id.searchView))
                .perform(typeText("b"), pressImeActionButton())
        onView(withId(R.id.clearSearchBox))
                .perform(click())
        onView(withId(R.id.searchView))
                .perform(pressImeActionButton())

        onView(withText("a")).check(matches(isDisplayed()))
        onView(withText("b")).check(matches(isDisplayed()))

        onView(withContentDescription(R.string.abc_action_bar_up_description))
                .perform(click())
        onView(withId(R.id.homeSearchView))
                .perform(click())
        onView(withText("a")).check(matches(isDisplayed()))
        onView(withText("b")).check(matches(isDisplayed()))

        onView(withText("a"))
                .perform(click())
        onView(withId(R.id.searchView))
                .check(matches(withText("a")))

        onView(withContentDescription(R.string.abc_action_bar_up_description))
                .perform(click())
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext())
        onView(withText(R.string.settings))
                .perform(click())
        onView(withText(R.string.clear_search_history))
                .perform((click()))
        Espresso.pressBack()
        onView(withId(R.id.homeSearchView))
                .perform(click())

        onView(withText("a")).check(doesNotExist())
        onView(withText("b")).check(doesNotExist())
    }

    @Test
    fun searchFail() {
        onView(withId(R.id.searchView))
                .perform(typeText("x1"), pressImeActionButton())
        onView(withText(R.string.no_results))
                .check(matches(isDisplayed()))
    }

    fun withSignId(id: Int): Matcher<Any> {
        return object : BoundedMatcher<Any, Sign>(Sign::class.java) {
            override fun matchesSafely(item: Sign?): Boolean {
                return id == item?.id ?: false
            }

            override fun describeTo(description: org.hamcrest.Description) {
                description.appendText("with id:")
                description.appendValue(id)
            }
        }
    }


    @Test
    fun searchResults() {
        onView(withId(R.id.searchView))
                .perform(typeText("restaurang"), pressImeActionButton())
        onView(withText("􌥔􌥘􌤭􌤹􌤭􌥓􌥘􌦎􌥼􌥹􌦉􌥯􌥿"))
                .check(matches(isDisplayed()))
        onView(withText("􌤴􌥗􌥌􌤹􌥌􌤴􌤶􌦎􌥼􌦄􌥹􌦉􌥼􌥻"))
                .check(matches(isDisplayed()))
        onView(withText("Egennamn » Restaurang"))
                .check(matches(isDisplayed()))
        onView(withId(R.id.list))
                .perform(RecyclerViewActions.scrollToHolder(
                        withExample(containsString("populär restaurang"))))
        onView(withSubstring("populär restaurang"))
                .check(matches(isDisplayed()))
    }

    @Test
    fun exampleActions() {
        val exampleString = "tycker att vlogg"
        onView(withId(R.id.searchView))
                .perform(typeText("teckenspr"), pressImeActionButton())
        onView(withId(R.id.list))
                .perform(RecyclerViewActions.scrollToHolder(
                        withExample(containsString(exampleString))))

        onView(withSubstring(exampleString))
                .perform(click())
        onView(withSubstring("individuell idrott"))
                .check(matches(isDisplayed()))
        onView(withId(R.id.list))
                .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(0))
        onView(withId(R.id.wordText))
                .check(matches(withText("vara ute efter")))
        Espresso.pressBack()
        onView(withSubstring(exampleString))
                .check(matches(isDisplayed()))

        onView(allOf(withId(R.id.exampleSearch),
                hasSibling(withSubstring(exampleString))))
                .perform(click())
        onView(withText("information"))
                .check(matches(isDisplayed()))
        onView(withText("sprida"))
                .check(matches(isDisplayed()))
        onView(withText("vlogg"))
                .check(matches(isDisplayed()))
        Espresso.pressBack()
        onView(withSubstring(exampleString))
                .check(matches(isDisplayed()))
    }


    private fun withExample(matcher: Matcher<String>): Matcher<ItemRecyclerViewAdapter.ExampleViewHolder> {
        return object : TypeSafeMatcher<ItemRecyclerViewAdapter.ExampleViewHolder>() {
            override fun matchesSafely(holder: ItemRecyclerViewAdapter.ExampleViewHolder): Boolean {
                return matcher.matches(holder.mIdView.text.toString())
            }

            override fun describeTo(description: Description) {
                description.appendText("with text: ")
                matcher.describeTo(description)
            }
        }
    }

    @After
    fun unregisterIdlingResource() {
        mIdlingResource?.let {
            IdlingRegistry.getInstance().unregister(it)
        }
    }
}