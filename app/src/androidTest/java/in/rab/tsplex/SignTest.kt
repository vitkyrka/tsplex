package `in`.rab.tsplex

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import androidx.test.InstrumentationRegistry
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.*
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@LargeTest
class SignTest {

    @get:Rule
    var activityScenarioRule = ActivityScenarioRule<SignActivity>(
            Intent(ApplicationProvider.getApplicationContext(), SignActivity::class.java)
                    .putExtra("url", "5705")
    )

    @Before
    fun setUp() {
        Intents.init()
        intending(not(isInternal())).respondWith(ActivityResult(Activity.RESULT_OK, null))
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun openInBrowser() {
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext())
        onView(withText(R.string.open_in_browser))
                .perform(click())
        intended(allOf(hasData("https://teckensprakslexikon.su.se/ord/05705/"),
                hasAction(Intent.ACTION_VIEW)))
    }

    @Test
    fun defaultVideo() {
        activityScenarioRule.scenario.onActivity { TestHelper.assertIsCurrentVideo(it, "05705-tecken") }
    }

    @Test
    fun descriptionClick() {
        onView(ViewMatchers.withId(R.id.list))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0,
                        click()))

        activityScenarioRule.scenario.onActivity { TestHelper.assertIsCurrentVideo(it, "05705-tecken") }
    }

    @Test
    fun examplePreviewClick() {
        onView(ViewMatchers.withId(R.id.list))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1,
                        TestHelper.clickPreview()))

        activityScenarioRule.scenario.onActivity { TestHelper.assertIsCurrentVideo(it, "05705-fras") }
    }

    @Test
    fun exampleClick() {
        onView(ViewMatchers.withId(R.id.list))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1,
                        click()))

        activityScenarioRule.scenario.onActivity { TestHelper.assertIsCurrentVideo(it, "05705-fras") }
    }
}