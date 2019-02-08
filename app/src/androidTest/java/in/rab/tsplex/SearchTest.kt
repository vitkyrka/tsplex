package `in`.rab.tsplex

import `in`.rab.tsplex.TestHelper.Companion.assertIsCurrentVideo
import `in`.rab.tsplex.TestHelper.Companion.clickPreview
import android.view.View
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import androidx.test.InstrumentationRegistry
import androidx.test.espresso.*
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.SimpleExoPlayerView
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.*
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@LargeTest
class SearchTest {

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

            it.mAutoSearch = false
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
        val exampleString = "nyss åkt till"
        onView(withId(R.id.searchView))
                .perform(typeText("Kanarie"), pressImeActionButton())
        onView(withId(R.id.list))
                .perform(RecyclerViewActions.scrollToHolder(
                        withExample(containsString(exampleString))))

        onView(withSubstring(exampleString))
                .perform(click())
        onView(withSubstring("Hen stannar i"))
                .check(matches(isDisplayed()))
        onView(withId(R.id.wordText))
                .check(matches(withText("fyra veckor")))
        Espresso.pressBack()
        onView(withSubstring(exampleString))
                .check(matches(isDisplayed()))

        onView(allOf(withId(R.id.exampleSearch),
                hasSibling(withSubstring(exampleString))))
                .perform(click())
        onView(withText("fyra veckor"))
                .check(matches(isDisplayed()))
        onView(withText("Kanarieöarna, Gran Canaria"))
                .check(matches(isDisplayed()))
        onView(withText("stanna kvar, uppehålla sig"))
                .check(matches(isDisplayed()))
        Espresso.pressBack()
        onView(withSubstring(exampleString))
                .check(matches(isDisplayed()))
    }

    private fun withPlaybackSpeed(speed: Float): Matcher<View> {
        return object : BoundedMatcher<View, SimpleExoPlayerView>(SimpleExoPlayerView::class.java) {
            override fun matchesSafely(view: SimpleExoPlayerView): Boolean {
                return view.player.playbackParameters.speed == speed
            }

            override fun describeTo(description: Description) {
                description.appendText("with playback speed: ")
                description.appendValue(speed)
            }
        }
    }

    private fun withRepeatMode(repeatMode: Int): Matcher<View> {
        return object : BoundedMatcher<View, SimpleExoPlayerView>(SimpleExoPlayerView::class.java) {
            override fun matchesSafely(view: SimpleExoPlayerView): Boolean {
                return view.player.repeatMode == repeatMode
            }

            override fun describeTo(description: Description) {
                description.appendText("with repeat mode: ")
                description.appendValue(repeatMode)
            }
        }
    }

    @Test
    fun videoPlaybackSpeed() {
        // This needs signs without a related tab, otherwise there will be two exoPlayerViews

        onView(withId(R.id.searchView))
                .perform(typeText("straffk"), pressImeActionButton())
        onView(withId(R.id.list))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0,
                        click()))

        onView(withId(R.id.exoPlayerExtraControls)).check(matches(not(isDisplayed())))

        onView(withId(R.id.exoPlayerView)).perform(click())
        onView(withId(R.id.exoPlayerExtraControls)).check(matches(isDisplayed()))

        onView(withId(R.id.exo_050x)).perform(click())
        onView(withId(R.id.exoPlayerView)).check(matches(withPlaybackSpeed(0.50f)))

        onView(withId(R.id.exo_075x)).perform(click())
        onView(withId(R.id.exoPlayerView)).check(matches(withPlaybackSpeed(0.75f)))

        Espresso.pressBack()

        onView(withId(R.id.list))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1,
                        click()))

        onView(withId(R.id.exoPlayerView)).check(matches(withPlaybackSpeed(0.75f)))

        onView(withId(R.id.exoPlayerView)).perform(click())
        onView(withId(R.id.exo_100x)).perform(click())
        onView(withId(R.id.exoPlayerView)).check(matches(withPlaybackSpeed(1.0f)))

        Espresso.pressBack()

        onView(withId(R.id.list))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0,
                        click()))

        onView(withId(R.id.exoPlayerView)).check(matches(withPlaybackSpeed(1.0f)))
        onView(withId(R.id.exoPlayerView)).perform(click())
        onView(withId(R.id.exo_050x)).perform(click())

        Espresso.pressBack()

        onView(withId(R.id.list))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(2,
                        clickPreview()))

        onView(withId(R.id.exoPlayerExtraControls)).check(matches(not(isDisplayed())))
        onView(withId(R.id.exoPlayerView)).check(matches(withPlaybackSpeed(0.5f)))
    }

    fun enableRepeat(): ViewAction {
        return object : ViewAction {
            override fun getDescription(): String {
                return "set selected"
            }

            override fun getConstraints(): Matcher<View> {
                return isAssignableFrom(ImageButton::class.java)
            }

            override fun perform(uiController: UiController?, view: View?) {
                if (view is ImageButton) {
                    if (view.contentDescription.contains("none"))
                        view.performClick()
                }
            }
        }
    }

    @Test
    fun videoRepeatMode() {
        // This needs signs without a related tab, otherwise there will be two exoPlayerViews

        onView(withId(R.id.searchView))
                .perform(typeText("straffk"), pressImeActionButton())
        onView(withId(R.id.list))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1,
                        click()))

        onView(withId(R.id.exoPlayerView)).perform(click())

        onView(withId(R.id.exoPlayerExtraControls)).check(matches(isDisplayed()))

        onView(withId(R.id.exo_repeat_toggle)).perform(enableRepeat())
        onView(withId(R.id.exoPlayerView)).check(matches(withRepeatMode(Player.REPEAT_MODE_ALL)))

        onView(withId(R.id.exo_repeat_toggle)).perform(click())
        onView(withId(R.id.exoPlayerView)).check(matches(withRepeatMode(Player.REPEAT_MODE_OFF)))

        Espresso.pressBack()

        onView(withId(R.id.list))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(2,
                        click()))

        onView(withId(R.id.exoPlayerView)).check(matches(withRepeatMode(Player.REPEAT_MODE_OFF)))

        onView(withId(R.id.exoPlayerView)).perform(click())
        onView(withId(R.id.exo_repeat_toggle)).perform(click())
        onView(withId(R.id.exoPlayerView)).check(matches(withRepeatMode(Player.REPEAT_MODE_ALL)))

        Espresso.pressBack()

        onView(withId(R.id.list))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(3,
                        clickPreview()))

        onView(withId(R.id.exoPlayerView)).check(matches(withRepeatMode(Player.REPEAT_MODE_ALL)))
    }

    @Test
    fun videoPreview() {
        onView(withId(R.id.searchView))
                .perform(typeText("handalfabet"), pressImeActionButton())

        onView(withId(R.id.list))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(4,
                        clickPreview()))

        activityScenarioRule.scenario.onActivity { assertIsCurrentVideo(it, "04843-tecken") }

        onView(withId(R.id.exoPlayerPrevious)).perform(click())
        activityScenarioRule.scenario.onActivity { assertIsCurrentVideo(it, "01557-tecken") }

        onView(withId(R.id.exoPlayerPrevious)).check(matches(not(isDisplayed())))

        onView(withId(R.id.exoPlayerNext)).perform(click())
        activityScenarioRule.scenario.onActivity { assertIsCurrentVideo(it, "04843-tecken") }

        onView(withId(R.id.exoPlayerNext)).perform(click())
        activityScenarioRule.scenario.onActivity { assertIsCurrentVideo(it, "11352-tecken") }

        onView(withId(R.id.exoPlayerNext)).perform(click())
        activityScenarioRule.scenario.onActivity { assertIsCurrentVideo(it, "04603-fras") }

        onView(withId(R.id.exoPlayerNext)).check(matches(not(isDisplayed())))

        onView(withId(R.id.exoPlayerPrevious)).perform(click())
        activityScenarioRule.scenario.onActivity { assertIsCurrentVideo(it, "11352-tecken") }
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
}