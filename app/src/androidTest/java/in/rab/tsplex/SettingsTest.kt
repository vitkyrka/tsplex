package `in`.rab.tsplex

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@LargeTest
class SettingsTest {

    @get:Rule
    var activityScenarioRule = ActivityScenarioRule<SettingsActivity>(SettingsActivity::class.java)

    @Test
    fun showLicenses() {
        onView(withText(R.string.open_source_licenses))
                .perform(click())
        onView(withText("absl")).perform(click())
        onView(withSubstring("Apache"))
                .check(matches(isDisplayed()))
    }
}