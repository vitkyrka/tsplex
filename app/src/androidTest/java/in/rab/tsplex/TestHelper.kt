package `in`.rab.tsplex

import android.view.View
import android.widget.ImageButton
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matcher
import org.hamcrest.Matchers

class TestHelper {
    companion object {
        fun clickPreview(): ViewAction {
            return object : ViewAction {
                override fun getDescription(): String {
                    return "click preview button"
                }

                override fun getConstraints(): Matcher<View> {
                    return ViewMatchers.isEnabled()
                }

                override fun perform(uiController: UiController?, view: View?) {
                    view?.findViewById<ImageButton>(R.id.playButton)?.let {
                        it.performClick()
                    }
                }

            }
        }

        fun clickExampleSearch(): ViewAction {
            return object : ViewAction {
                override fun getDescription(): String {
                    return "click preview button"
                }

                override fun getConstraints(): Matcher<View> {
                    return ViewMatchers.isEnabled()
                }

                override fun perform(uiController: UiController?, view: View?) {
                    view?.findViewById<ImageButton>(R.id.exampleSearch)?.let {
                        it.performClick()
                    }
                }

            }
        }

        fun assertIsCurrentVideo(activity: RoutingAppCompactActivity, video: String) {
            ViewMatchers.assertThat(activity.mCurrentVideo, Matchers.containsString(video))
        }
    }
}